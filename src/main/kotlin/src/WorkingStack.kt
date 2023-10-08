package src

import domain.Node
import domain.RightUnit
import domain.State
import domain.StateType.NON_TERMINAL
import domain.StateType.TERMINAL
import domain.Tree

data class WorkingStack(
    private val input: ArrayDeque<String>,
    private val automaton: Automaton,
    private val nonTerminalToNodeMapper: HashMap<String, Node>,
    private val terminalToNodeMapper: HashMap<String, Node>,
    private val regexToNodeMapper: HashMap<Regex, Node>,
) {
    private val nodeToTerminalMapper = terminalToNodeMapper.entries.associate { it.value to it.key }
    private val nodeToNonTerminalMapper = nonTerminalToNodeMapper.entries.associate { it.value to it.key }
    private val nodeToRegexMapper = regexToNodeMapper.entries.associate { it.value to it.key }
    private var trees = mutableListOf<Tree>()
    private var workingStack: MutableList<Node> = ArrayList()
    private var currentStates = automaton.startedStates

    private fun toStateType(isTerminalState: Boolean) =
        if (isTerminalState) TERMINAL else NON_TERMINAL

    fun process(): Tree {
        while (input.isNotEmpty() || workingStack != listOf(automaton.startState.rule.left)) {
            val statesByType = currentStates.groupBy { toStateType(it.isTerminalState) }
            val terminalStates = statesByType.getOrDefault(TERMINAL, emptyList())
            val nonTerminalStates = statesByType.getOrDefault(NON_TERMINAL, emptyList())

            when {
                terminalStates.size > 1 ->
                    resolveShiftShift()

                terminalStates.isNotEmpty() && nonTerminalStates.isNotEmpty() ->
                    resolveShiftReducing(terminalStates[0])

                terminalStates.isEmpty() ->
                    shift()

                else -> reduce(terminalStates[0])
            }
        }

        assert(trees.size == 1)
        println("Success processing")
        println("Result: ${trees[0].value}")
        return trees[0]
    }

    private fun shift() {
        if (input.isEmpty()) {
            trees.add(Tree(nodeToTerminalMapper[automaton.end]!!.toString()))
            workingStack.add(automaton.end)
        } else {
            val top = input.removeFirst()
            val topNode = terminalToNodeMapper[top] ?: regexToNodeMapper.entries.first { (regex, _) ->
                top.matches(regex)
            }.value
            trees.add(Tree(top, top))
            workingStack.add(topNode)
        }

        currentStates = automaton.statesAfterTransitions(workingStack)
    }

    private fun reduce(state: State): Unit = with(state.rule) {
        val treesInState = right.map {
            if (it.isTerminal) {
                if (nodeToTerminalMapper[it] != null) {
                    RightUnit(isRegex = false, nodeToTerminalMapper[it]!!)
                } else {
                    RightUnit(isRegex = true, nodeToRegexMapper[it]!!.toString())
                }
            } else {
                RightUnit(isRegex = false, nodeToNonTerminalMapper[it]!!)
            }
        }
        val diff = buildList {
            for (i in 1..(workingStack.size - right.size)) add(Node(-1, true))
        }
        val treesDiff = buildList {
            for (i in 1..(trees.size - right.size)) add(RightUnit(isRegex = false, ""))
        }

        workingStack = workingStack.zip(diff + right)
            .dropLastWhile { (nodeInStack, nodeInState) ->
                nodeInStack == nodeInState
            }
            .unzip().first as MutableList

        val children = trees.zip(treesDiff + treesInState)
            .takeLastWhile { (tree, treeInState) ->
                ((treeInState.isRegex && tree.node.matches(treeInState.representation.toRegex()))
                        || (tree.node == treeInState.representation))

            }
            .unzip().first as MutableList

        trees = trees.dropLast(children.size).toMutableList()
        workingStack.add(left)
        trees.add(
            Tree(
                nodeToNonTerminalMapper[left]!!,
                function?.call(children.map { it.value }.toTypedArray())
                    ?: if (children.size == 1 && children[0].value != null) children[0].value else null,
                (if (right != listOf(automaton.e)) children else listOf(
                    Tree(nodeToTerminalMapper[automaton.e]!!)
                )) as MutableList
            )
        ) // context-free grammar
        currentStates = automaton.statesAfterTransitions(workingStack)
    }

    private fun resolveShiftReducing(state: State) {
        if (input.isEmpty()) {
            reduce(state)
        } else {
            val node = terminalToNodeMapper[input.first()] ?: regexToNodeMapper.entries.first { (regex, _) ->
                input.first().matches(regex)
            }.value
            if (node in state.lookahead) {
                reduce(state)
            } else {
                shift()
            }
        }
    }

    private fun resolveShiftShift(): Nothing =
        throw IllegalStateException("Can`t resolve shift-shift conflict")

}