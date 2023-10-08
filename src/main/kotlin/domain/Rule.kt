package domain

import kotlin.reflect.KFunction

data class Rule(
    val left: Node,
    val right: List<Node>,
    val function: KFunction<Any>? = null,
)