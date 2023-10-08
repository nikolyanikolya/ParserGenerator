package src

import domain.*

class AutomatonBuilder(
    private val grammar: TokenizedGrammar,
) {
    fun build(): Automaton = with(grammar) {
        val e = grammar.terminalToNode["empty"]!!
        val nka = HashMap<StateWithTransition, List<State>>()
        val right = rules[start]!!.rulesRight.map { it.rightNodes }.flatten()
        val reduceFunction = rules[start]!!.rulesRight.map { it.reduceFunction }
        assert(reduceFunction.size == 1)
        val startState = State(0, Rule(start, right, reduceFunction[0]), setOf(end), right.isEmpty())
        val visited = HashSet<RawState>()
        val deque = ArrayDeque(listOf(RawState(startState)))

        while (!deque.isEmpty()) {
            val curState = deque.removeFirst()
            if (curState.marker >= curState.rule.right.size) {
                continue
            }
            with(curState) {
                val node = rule.right[marker]
                val result = mutableListOf<State>()

                if (!node.isTerminal) {
                    rules[node]!!.rulesRight
                        .forEach { ruleVariant ->
                            val to = State(
                                0,
                                Rule(node, ruleVariant.rightNodes, ruleVariant.reduceFunction),
                                lookahead.let {
                                    var curLA = setOf<Node>()
                                    for (i in 1..rule.right.size) {
                                        if (marker + i >= rule.right.size) {
                                            curLA = curLA.plus(it)
                                            break
                                        } else {
                                            curLA = curLA.plus(first(rule.right[marker + i]))
                                        }

                                        if (e !in first(rule.right[marker + i]))
                                            break
                                    }
                                    curLA
                                },
                                ruleVariant.rightNodes.isEmpty()
                            )
                            val rawTo = RawState(to)
                            if (rawTo !in visited) {
                                deque.add(rawTo)
                                visited.add(rawTo)
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
                    nka[StateWithTransition(this.toState(), e)] = result
                }

                val rawTo = RawState(to)
                if (rawTo !in visited) {
                    deque.add(rawTo)
                    visited.add(rawTo)
                }

                nka[StateWithTransition(this.toState(), node)] = listOf(to)
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
                rule.value.rulesRight.map { it.rightNodes }.forEach { nodes ->
                    result.add(nodes[0])
                }
            }
        }

        return result
    }
}

class Automaton(
    val nka: HashMap<StateWithTransition, List<State>>,
    val e: Node,
    val end: Node,
    val startState: State
) {
    val startedStates: List<State> = statesAfterEpsTransitions(startState)

    private fun statesAfterTransition(transition: Node, initialStates: List<State> = startedStates): List<State> {
        val states = mutableSetOf<State>()
        initialStates.forEach {
            if (nka.containsKey(StateWithTransition(it, transition))) {
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
        nka.entries.joinToString("==============\n") { (key, value) ->
            value.joinToString("\n") {
                key.state.toString() + " by " + key.node + ">>>> " + it.toString()
            }
        }
}