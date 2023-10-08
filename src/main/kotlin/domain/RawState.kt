package domain

data class RawState(
    val marker: Int,
    val rule: Rule,
    val lookahead: Set<Node>,
    val isTerminalState: Boolean,
) {
    constructor(state: State) : this(state.marker, state.rule, state.lookahead, state.isTerminalState)

    fun toState(): State = State(marker, rule, lookahead, isTerminalState)
}