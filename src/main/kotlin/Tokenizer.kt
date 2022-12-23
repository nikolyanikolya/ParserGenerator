import java.lang.reflect.Modifier
import java.nio.file.Files.newBufferedWriter
import java.nio.file.Path
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinProperty

object Tokenizer {
    fun tokenize(input: List<String>): TokenizedGrammar {
        val fileWithFunctions = "src/main/kotlin/functions.kt"
        val terminalToNode = HashMap<String, Node>()
        newBufferedWriter(Path.of(fileWithFunctions)).use { writer ->
                writer.write("import kotlin.reflect.KFunction")
                writer.newLine()
                val allFunctions = mutableListOf<String>()
                input.forEach {
                    val res = it.split("@", limit = 3)
                    if (res.size > 1) {
                        writer.write(res[1])
                        val functionName = res[1].trim().split(" ")[1]
                        allFunctions.add("::$functionName")
                        writer.newLine()
                    }
                }
                writer.write("val functions : Array<KFunction<Any>> = arrayOf(${allFunctions.joinToString(",")})")
                writer.newLine()

            }

        val functions = getFieldFromFile("FunctionsKt", "functions")!!.call()
        var cnt = 0L
        val nonTerminalToNode = HashMap<String, Node>().let { map ->
            input.forEach {
                val res = it.split("@", limit = 3)
                val (left, _) = res[0].split(":", limit = 2)
                if (!left.startsWith("Start") && !left.startsWith("End")) {
                    map.putIfAbsent(left, Node(cnt++, false)) // context-free grammar
                }
            }
            map
        }

        val regexToNode = HashMap<Regex, Node>()
        val rules = HashMap<Node, RuleVariants>()
        val (_, startedState) = input[0].split(" ", limit = 2)
        val startedNode = nonTerminalToNode[startedState]!!

        val (_, endState) = input[1].split(" ", limit = 2)
        val endNode = Node(cnt++,  true)
        terminalToNode.putIfAbsent(endState, endNode)
        var functionCnt = 0

        for (str in input.drop(2)) {
            val ruleVariants = RuleVariants(mutableListOf())
            val res = str.split("@", limit = 3)
            val (left, right) = res[0].split(":", limit = 2)
            val leftToken = nonTerminalToNode[left]!!
            val rightRules = right.split("|")

            for (rightRule in rightRules) {
                val rightParts = rightRule.trim().split(" ")
                val tokens = mutableListOf<Node>()

                for (rightUnit in rightParts) {
                    val trimmedUnit = rightUnit.trim()
                    if (!nonTerminalToNode.containsKey(trimmedUnit)) {
                        if (left.matches("^[A-Z].*".toRegex())) {
                            val entry = regexToNode.entries.firstOrNull { (regex, _) -> trimmedUnit == regex.toString() }
                            if (entry == null) {
                                val newNode = Node(cnt++, true)
                                regexToNode[trimmedUnit.toRegex()] = newNode
                                tokens.add(newNode)
                            } else {
                                tokens.add(entry.value)
                            }
                        } else {
                            terminalToNode.putIfAbsent(trimmedUnit, Node(cnt++, true))
                            tokens.add(terminalToNode[trimmedUnit]!!)
                        }
                    } else {
                        tokens.add(nonTerminalToNode[trimmedUnit]!!)
                    }
                }

                if (res.size > 1) {
                    ruleVariants.add(tokens, functions[functionCnt++])
                } else {
                    ruleVariants.add(tokens)
                }

                if (rules.containsKey(leftToken)) {
                    ruleVariants.plus(rules[leftToken]!!)
                }
            }

            rules[leftToken] = ruleVariants
        }

        return TokenizedGrammar(startedNode, endNode, nonTerminalToNode, terminalToNode, regexToNode, rules)
    }
}

data class Node(
    val token: Long,
    val isTerminal: Boolean,
)

data class RuleVariants(
    val rulesRight: MutableList<RuleVariant>
) {
    fun add(rule: List<Node>, function: KFunction<Any>? = null) {
        rulesRight.add(RuleVariant(rule, function))
    }

    fun plus(rules: RuleVariants) {
        rulesRight += rules.rulesRight
    }
}

data class RuleVariant(
    val rightNodes: List<Node>,
    val reduceFunction: KFunction<Any>? = null
)

data class TokenizedGrammar(
    val start: Node,
    val end: Node,
    val nonTerminalToNode: HashMap<String, Node>,
    val terminalToNode: HashMap<String, Node>,
    val regexToNode: HashMap<Regex, Node>,
    val rules: HashMap<Node, RuleVariants>,
) {
    fun writeRules(fileName: Path) {
        newBufferedWriter(fileName).use {
            rules.entries.forEach { (key, value) ->
                it.write("${key.token} -> " +
                        value.rulesRight.joinToString(" | ") {
                            it.rightNodes.joinToString(" ") { node -> node.token.toString() }
                        }
                )
                it.newLine()
            }
        }
    }

    fun writeTokens(fileName: Path) {
        newBufferedWriter(fileName).use {
            (terminalToNode.entries + nonTerminalToNode.entries + regexToNode.entries).forEach { (key, value) ->
                it.write("$key = $value")
                it.newLine()
            }
        }
    }
}

fun getFieldFromFile(fileName: String, fieldName: String): KProperty<Array<KFunction<Any>>>? {
    val selfRef = ::getFieldFromFile
    val currentClass = selfRef.javaMethod!!.declaringClass
    val classDefiningFunctions = currentClass.classLoader.loadClass(fileName)
    val javaField = classDefiningFunctions.declaredFields.find { it.name == fieldName && Modifier.isStatic(it.modifiers) }
    return javaField?.kotlinProperty as KProperty<Array<KFunction<Any>>>?
}
