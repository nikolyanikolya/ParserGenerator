import java.nio.file.Files.newBufferedWriter
import java.nio.file.Path

object Tokenizer {
    fun tokenize(input: List<String>): TokenizedGrammar {
        var cnt = 0L
        val terminalToNode = HashMap<String, Node>()
        val nonTerminalToNode = HashMap<String, Node>().let { map ->
            input.forEach {
                val (left, _) = it.split(":")
                if (!left.startsWith("Start") && !left.startsWith("End"))
                  map.putIfAbsent(left, Node(cnt++, false)) // context-free grammar
            }
            map
        }
        val regexToNode = HashMap<Regex, Node>()
        val rules = HashMap<Node, RuleVariants>()
        val (_, startedState) = input[0].split(" ", limit = 2)
        val startedNode = nonTerminalToNode[startedState]!!

        val (_, endState) = input[1].split(" ", limit = 2)
        val endNode = Node(cnt++, true)
        terminalToNode.putIfAbsent(endState, endNode)

        for (str in input.drop(2)) {
            val ruleVariants = RuleVariants(mutableListOf())
            val (left, right) = str.split(":", limit = 2)
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

                ruleVariants.add(tokens)
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
    val rulesRight: MutableList<List<Node>>
) {
    fun add(rule: List<Node>) {
        rulesRight.add(rule)
    }

    fun plus(rules: RuleVariants) {
        rulesRight += rules.rulesRight
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
    fun writeRules(fileName: Path) {
        newBufferedWriter(fileName).use {
            rules.entries.forEach { (key, value) ->
                it.write("${key.token} -> " +
                        value.rulesRight.joinToString(" | ") {
                            it.joinToString(" ") { node -> node.token.toString() }
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
