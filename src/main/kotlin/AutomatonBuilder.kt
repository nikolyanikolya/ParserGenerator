data class AutomatonBuilder(
    private val grammar: TokenizedGrammar,
) {
    fun build(): Automaton = with(grammar) {
        val e = grammar.terminalToNode["empty"]!!
        val nka = HashMap<StateWithTransition, List<State>>()
        val right = rules[start]!!.rulesRight.flatten()
        val startState = State(0, Rule(start, right), setOf(end), right.isEmpty())
        val visited = HashSet<State>()
        val deque = ArrayDeque(listOf(startState))

        while (!deque.isEmpty()) {
            val curState = deque.removeFirst()
            if (curState.marker >= curState.rule.right.size) {
                continue
            }
            with(curState) {
                val node = rule.right[marker]
                val result = mutableListOf<State>()

                if (!node.isTerminal) {
                    rules[node]!!.rulesRight.forEach { rightNodes ->
                        val to = State(
                            0,
                            Rule(node, rightNodes),
                            lookahead.let {
                                if (marker + 1 >= rule.right.size)
                                    it
                                else
                                    it.plus(first(rule.right[marker + 1]))
                            },
                            rightNodes.isEmpty()
                        )
                        if (to !in visited) {
                            deque.add(to)
                            visited.add(to)
                        }
                        result.add(to)
                    }
                }

                val to = State(
                    marker + 1,
                    rule,
                    lookahead,
                    marker + 1 == rule.right.size
                )

                if (result.isNotEmpty()) {
                    nka.putIfAbsent(StateWithTransition(this, e), result)
                }

                if (to !in visited) {
                    deque.add(to)
                    visited.add(to)
                }
                nka.putIfAbsent(StateWithTransition(this, node), listOf(to))
            }
        }

        println("Success building automaton")
        return@with Automaton(nka, e, end, startState)
    }

    private fun first(node: Node): Set<Node> {
        if (node.isTerminal)
            return setOf(node)

        val result = mutableSetOf<Node>()
        for (rule in grammar.rules) {
            if (rule.key == node) {
                rule.value.rulesRight.forEach { nodes ->
                    result.add(nodes[0])
                }
            }
        }

        return result
    }

    private fun follow(node: Node): Set<Node> {
        val result = mutableSetOf<Node>()

        for (rule in grammar.rules) {
            rule.value.rulesRight.forEach { nodes ->
                if (node == nodes.last()) {
                    result.addAll(follow(rule.key))
                } else {
                    val indexOfFirstEntry = nodes.indexOfFirst { it == node }
                    if (indexOfFirstEntry != -1) {
                        result.addAll(first(nodes[indexOfFirstEntry + 1]))
                    }
                }
            }
        }

        return result
    }
}

data class Automaton(
    val nka: HashMap<StateWithTransition, List<State>>,
    val e: Node,
    val end: Node,
    val startState: State
) {
    val startedStates: List<State> = statesAfterEpsTransitions(startState)

    private fun statesAfterTransition(transition: Node, initialStates: List<State> = startedStates): List<State> {
        val states = mutableSetOf<State>()
        initialStates.forEach {
            if(nka.containsKey(StateWithTransition(it, transition))) {
                nka[StateWithTransition(it, transition)]!!.forEach { state ->
                    states.addAll(statesAfterEpsTransitions(state))
                }
            }
        }
       return states.toList()
    }

    fun statesAfterTransitions(transitions: List<Node>): List<State> {
        var curStates = startedStates
        transitions.forEach {
            curStates = statesAfterTransition(it, curStates)
        }
        return curStates
    }

    private fun statesAfterEpsTransitions(state: State): List<State> {
        val deque = ArrayDeque(listOf(state))
        val visited = mutableListOf(state)
        while (deque.isNotEmpty()) {
            val curState = deque.removeFirst()
            if (nka.containsKey(StateWithTransition(curState, e))) {
                nka[StateWithTransition(curState, e)]!!.forEach {
                    if (it !in visited) {
                        deque.add(it)
                        visited.add(it)
                    }
                }
            }
        }
        return visited
    }

    override fun toString(): String =
        nka.entries.joinToString("==============\n") {
            (key, value) ->
            value.joinToString("\n") {
                key.state.toString() + " by " + key.node + ">>>> " + it.toString()
            }
        }


}

data class State(
    val marker: Int,
    val rule: Rule,
    val lookahead: Set<Node>,
    val isTerminalState: Boolean,
) {
    override fun toString(): String {
        return "[${rule.left.token} -> " +
                    "${rule.right.joinToString(",") { it.token.toString() }}, " +
                    "$marker, $lookahead" +
                "]" +
                if (isTerminalState) " TERMINATE" else ""
    }
}

data class Rule(
    val left: Node,
    val right: List<Node>
)

data class StateWithTransition(
    val state: State,
    val node: Node,
)