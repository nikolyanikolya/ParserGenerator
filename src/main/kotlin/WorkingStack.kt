import WorkingStack.StateType.NON_TERMINAL
import WorkingStack.StateType.TERMINAL

data class WorkingStack(
    private val input: ArrayDeque<String>,
    private val automaton: Automaton,
    private val inputToNodeMapper: HashMap<String, Node>,
) {
    private val nodeToInputMapper = inputToNodeMapper.entries.associate { it.value to it.key  }
    private var trees = mutableListOf<Tree>()
    private var workingStack: MutableList<Node> = ArrayList()
    private var currentStates = automaton.startedStates
    enum class StateType {
        TERMINAL,
        NON_TERMINAL;
    }

    private fun toStateType(isTerminalState: Boolean) =
        if (isTerminalState) TERMINAL else NON_TERMINAL
    fun process() {
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
        trees[0].display()
        println("Success processing")
    }
    private fun shift() {
        if (input.isEmpty()) {
            trees.add(Tree(nodeToInputMapper[automaton.end]!!))
            workingStack.add(automaton.end)
        } else {
            val top = input.removeFirst()
            val topNode = inputToNodeMapper[top] ?: throw IllegalStateException("illegal symbol: $top")
            trees.add(Tree(top))
            workingStack.add(topNode)
        }

        currentStates = automaton.statesAfterTransitions(workingStack)
    }
    private fun reduce(state: State): Unit = with(state.rule) {
        val treesInState = right.map { Tree(nodeToInputMapper[it]!!) }
        val diff = buildList {
            for(i in 1..(workingStack.size - right.size)) add(Node(-1, true))
        }
        val treesDiff =  buildList {
            for(i in 1..(trees.size - right.size)) add(Tree(""))
        }

        workingStack = workingStack.zip(diff + right)
            .dropLastWhile { (nodeInStack, nodeInState) ->
                nodeInStack == nodeInState
            }
            .unzip().first as MutableList

        val children = trees.zip(treesDiff + treesInState)
            .takeLastWhile { (tree, treeInState) ->
                tree.node == treeInState.node
            }
            .unzip().first as MutableList

        trees = trees.dropLast(children.size).toMutableList()

        workingStack.add(left)
        trees.add(Tree(nodeToInputMapper[left]!!, children))
        currentStates = automaton.statesAfterTransitions(workingStack)
    }

    private fun resolveShiftReducing(state: State) =
        if (input.isEmpty() || inputToNodeMapper[input.first()] in state.lookahead) {
            reduce(state)
        } else {
            shift()
        }

    private fun resolveShiftShift(): Nothing =
        throw IllegalStateException("Can`t resolve shift-shift conflict")

}