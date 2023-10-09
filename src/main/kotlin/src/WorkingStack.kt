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

        require(trees.size == 1)
        println("Success processing")
        println("Result: ${trees[0].value}")
        return trees[0]
    }

    private fun shift() {
        var top = ""
        while (input.isNotEmpty()) {
            top += input.removeFirst()
            val topNode = terminalToNodeMapper[top] ?: regexToNodeMapper.entries.firstOrNull { (regex, _) ->
                top.matches(regex)
            }?.value ?: continue
            trees.add(Tree(top, top))
            workingStack.add(topNode)
            currentStates = automaton.statesAfterTransitions(workingStack)
            return
        }
        trees.add(Tree(nodeToTerminalMapper[automaton.end]!!.toString()))
        workingStack.add(automaton.end)

        currentStates = automaton.statesAfterTransitions(workingStack)
    }

    private fun reduce(state: State): Unit = with(state.rule) {
        val rightUnits = rightUnits(state)
        val workingStackDiff = List(workingStack.size - right.size) { Node(-1, true) }
        val rightUnitsDiff = List(trees.size - right.size) { RightUnit(isRegex = false, "") }

        workingStack = workingStack.zip(workingStackDiff + right)
            .dropLastWhile { (nodeInStack, nodeInState) ->
                nodeInStack == nodeInState
            }
            .unzip().first as MutableList

        val children = trees.zip(rightUnitsDiff + rightUnits)
            .takeLastWhile { (tree, rightUnit) ->
                ((rightUnit.isRegex && tree.node.matches(rightUnit.representation.toRegex()))
                        || (tree.node == rightUnit.representation))

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
        )
        currentStates = automaton.statesAfterTransitions(workingStack)
    }

    private fun resolveShiftReducing(state: State) {
        val inputCopy = ArrayDeque(input)
        var top = ""
        while (inputCopy.isNotEmpty()) {
            top += inputCopy.removeFirst()
            val node = terminalToNodeMapper[top] ?: regexToNodeMapper.entries.firstOrNull { (regex, _) ->
                top.matches(regex)
            }?.value ?: continue
            if (node in state.lookahead) {
                reduce(state)
            } else {
                shift()
            }
            return
        }
        input.clear()
        reduce(state)
    }

    private fun rightUnits(state: State): List<RightUnit> = with(state.rule) {
        right.map {
            if (it.isTerminal) {
                if (nodeToTerminalMapper[it] != null) {
                    RightUnit(isRegex = false, nodeToTerminalMapper[it]!!)
                } else if (nodeToRegexMapper[it] != null) {
                    RightUnit(isRegex = true, nodeToRegexMapper[it]!!.toString())
                } else {
                    throw IllegalStateException("unexpected token found")
                }
            } else {
                RightUnit(
                    isRegex = false, nodeToNonTerminalMapper[it]
                        ?: throw IllegalStateException("unexpected token found")
                )
            }
        }
    }

    private fun resolveShiftShift(): Nothing =
        throw IllegalStateException("Can`t resolve shift-shift conflict")

}