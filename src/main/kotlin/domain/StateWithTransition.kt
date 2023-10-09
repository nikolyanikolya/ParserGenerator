package domain

data class StateWithTransition(
    val state: State,
    val node: Node?,
)