package src

import domain.ApplicationNeedRestartException
import domain.Node
import domain.RuleVariants
import utils.ReflectionUtils.obtainJavaClass
import utils.ReflectionUtils.obtainReduceFunctions
import java.nio.file.Files
import java.nio.file.Files.newBufferedWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

object Tokenizer {
    fun tokenize(input: List<String>, reduceFunctionsFileName: String): TokenizedGrammar {
        val reduceFunctionsPath = Path(reduceFunctionsFileName)
        val reduceFunctionsJavaClass = obtainJavaClass(reduceFunctionsPath)
        val reduceFunctionFileExists = Files.exists(reduceFunctionsPath)

        val terminalToNode = HashMap<String, Node>()
        if (!reduceFunctionFileExists) {
            ReduceFunctionsParser.parseAndSaveToFile(input, reduceFunctionsPath)
            throw ApplicationNeedRestartException("Reduce functions were parsed," +
                    " now, please ensure that functions are correct and restart application"
            )
        }
        val reduceFunctions = obtainReduceFunctions(
            reduceFunctionsJavaClass,
            reduceFunctionsPath.nameWithoutExtension
        )!!.call()
        Files.delete(reduceFunctionsPath)

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
                    ruleVariants.add(tokens, reduceFunctions[functionCnt++])
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

data class TokenizedGrammar(
    val start: Node,
    val end: Node,
    val nonTerminalToNode: HashMap<String, Node>,
    val terminalToNode: HashMap<String, Node>,
    val regexToNode: HashMap<Regex, Node>,
    val rules: HashMap<Node, RuleVariants>,
) {
    fun logRules(fileName: Path) {
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

    fun logTokens(fileName: Path) {
        newBufferedWriter(fileName).use {
            (terminalToNode.entries + nonTerminalToNode.entries + regexToNode.entries).forEach { (key, value) ->
                it.write("$key = $value")
                it.newLine()
            }
        }
    }
}
