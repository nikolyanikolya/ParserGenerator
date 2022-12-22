import java.nio.file.Files.newBufferedWriter
import java.nio.file.Path

object Tokenizer {
    fun tokenize(input: List<String>): TokenizedGrammar {
        var cnt = 0L
        val terminalToNode = HashMap<String, Node>()
        val nonTerminalToNode = HashMap<String, Node>()
        val regexToNode = HashMap<Regex, Node>()
        val rules = HashMap<Node, RuleVariants>()
        val (_, startedState) = input[0].split(" ")
        val startedNode = Node(cnt++, false)
        nonTerminalToNode.putIfAbsent(startedState, startedNode)

        val (_, endState) = input[1].split(" ")
        val endNode = Node(cnt++, true)
        terminalToNode.putIfAbsent(endState, endNode)

        for (str in input.drop(2)) {
            val ruleVariants = RuleVariants(mutableListOf())
            val (left, right) = str.split(":")
            nonTerminalToNode.putIfAbsent(left, Node(cnt++, false))
            val leftToken = nonTerminalToNode[left]!!
            val rightRules = right.split("|")

            for (rightRule in rightRules) {
                val rightPart = rightRule.trim().split(" ")
                val tokens = mutableListOf<Node>()

                for (rightUnit in rightPart) {
                    val trimmedUnit = rightUnit.trim()
                    if (trimmedUnit.matches("\'.*\'".toRegex())) {
                        terminalToNode.putIfAbsent(trimmedUnit, Node(cnt++, true))
                        tokens.add(terminalToNode[trimmedUnit]!!)
                    } else if (left.matches("^[A-Z].*".toRegex())) {
                        val entry = regexToNode.entries.firstOrNull { (regex, _) -> trimmedUnit.matches(regex) }
                        if (entry == null) {
                            val newNode = Node(cnt++, true)
                            regexToNode[trimmedUnit.toRegex()] = newNode
                            tokens.add(newNode)
                        } else {
                            tokens.add(entry.value)
                        }
                    } else {
                        nonTerminalToNode.putIfAbsent(trimmedUnit, Node(cnt++, false))
                        tokens.add(nonTerminalToNode[trimmedUnit]!!)
                    }

                }

                ruleVariants.add(tokens)
                if (nonTerminalToNode.containsKey(left) && rules.containsKey(leftToken)) {
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
