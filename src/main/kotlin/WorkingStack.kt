import WorkingStack.StateType.NON_TERMINAL
import WorkingStack.StateType.TERMINAL

data class WorkingStack(
    private val input: ArrayDeque<String>,
    private val automaton: Automaton,
    private val inputToNodeMapper: HashMap<String, Node>,
) {
    private var workingStack: MutableList<Node> = ArrayList()
    private var currentStates = automaton.startedStates()
    enum class StateType {
        TERMINAL,
        NON_TERMINAL;
    }
    private fun toStateType(isTerminalState: Boolean) =
        if (isTerminalState) TERMINAL else NON_TERMINAL
    fun process() {
        while (input.isNotEmpty() && workingStack != listOf(automaton.startState)) {
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
        println("Success processing")
    }
    private fun shift() {
        if (input.isNotEmpty()) {
            val top = input.removeFirst()
            workingStack.add(inputToNodeMapper[top] ?: throw IllegalStateException("illegal symbol: $top"))
        }

        workingStack.forEach { nodeInStack ->
            val newStates = mutableListOf<State>()
            currentStates.forEach { automatonState ->
                with(automaton.nka[StateWithTransition(automatonState, nodeInStack)]) {
                    if (this != null) {
                        newStates.addAll(this)
                    }
                }
            }
            currentStates = newStates
        }
    }
    private fun reduce(state: State): Unit = with(state.rule) {
        workingStack = workingStack.zip(right)
            .dropLastWhile { (nodeInStack, nodeInState) ->
                nodeInStack == nodeInState
            }
            .unzip().first as MutableList

        workingStack.add(left)
    }

    private fun resolveShiftReducing(state: State) =
        if (inputToNodeMapper[input.first()] in state.lookahead) {
            reduce(state)
        } else {
            shift()
        }

    private fun resolveShiftShift(): Nothing =
        throw IllegalStateException("Can`t resolve shift-shift conflict")

}