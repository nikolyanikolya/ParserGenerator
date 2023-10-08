package domain

data class State(
    val marker: Int,
    val rule: Rule,
    val lookahead: Set<Node>,
    val isTerminalState: Boolean,
) {
    override fun toString(): String {
        return "[${rule.left.token} -> " +
                "${rule.right.joinToString { it.token.toString() }}; " +
                "Marker: $marker; Lookahead: ${lookahead.joinToString { it.token.toString() }}" +
                "]" +
                if (isTerminalState) " TERMINATE STATE" else ""
    }

    override fun equals(other: Any?): Boolean {
        if (other is State) {
            return marker == other.marker && rule == other.rule && isTerminalState == other.isTerminalState
        }
        return false
    }

    override fun hashCode(): Int {
        return 239 * marker.hashCode() + 17 * rule.hashCode() + 13 * isTerminalState.hashCode()
    }
}