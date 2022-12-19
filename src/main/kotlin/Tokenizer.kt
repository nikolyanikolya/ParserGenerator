import java.nio.file.Files
import java.nio.file.Path

object Tokenizer {
    fun tokenize(input: List<String>): TokenizedGrammar {
        var cnt = 0L
        val inputToNode = HashMap<String, Node>()
        val rules = HashMap<Node, RuleVariants>()
        val (_, startedState) = input[0].split(" ")
        val startedNode = Node(cnt++, false)
        inputToNode.putIfAbsent(startedState, startedNode)

        val (_, endState) = input[1].split(" ")
        val endNode = Node(cnt++, true)
        inputToNode.putIfAbsent(endState, endNode)

        for (str in input.drop(2)) {
            val ruleVariants = RuleVariants(mutableListOf())
            val (left, right) = str.split(":")
            inputToNode.putIfAbsent(left, Node(cnt++, false))
            val leftToken = inputToNode[left]!!
            val rightRules = right.split("|")

            for (rightRule in rightRules) {
                val rightPart = rightRule.trim().split(" ")
                val tokens = mutableListOf<Node>()

                for (rightUnit in rightPart) {
                    val trimmedUnit = rightUnit.trim()
                    if (trimmedUnit.matches("\'.*\'".toRegex())) {
                        inputToNode.putIfAbsent(trimmedUnit, Node(cnt++, true))
                    } else {
                        inputToNode.putIfAbsent(trimmedUnit, Node(cnt++, false))
                    }

                    tokens.add(inputToNode[trimmedUnit]!!)
                }

                ruleVariants.add(tokens)
                if (inputToNode.containsKey(left) && rules.containsKey(leftToken)) {
                    ruleVariants.plus(rules[leftToken]!!)
                }
            }

            rules[leftToken] = ruleVariants
        }

        return TokenizedGrammar(startedNode, endNode, inputToNode, rules)
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
    val inputToNode: HashMap<String, Node>,
    val rules: HashMap<Node, RuleVariants>,
) {
    fun writeRules(fileName: Path) {
        Files.newBufferedWriter(fileName).use {
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
        Files.newBufferedWriter(fileName).use {
            inputToNode.entries.forEach { (key, value) ->
                it.write("$key = $value")
                it.newLine()
            }
        }
    }
}