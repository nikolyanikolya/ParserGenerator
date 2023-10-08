package domain

import kotlin.reflect.KFunction

class RuleVariant(
    val rightNodes: List<Node>,
    val reduceFunction: KFunction<Any>? = null
)

data class RuleVariants(
    val rulesRight: MutableList<RuleVariant>
) {
    fun add(rule: List<Node>, function: KFunction<Any>? = null) {
        rulesRight.add(RuleVariant(rule, function))
    }

    fun plus(rules: RuleVariants) {
        rulesRight += rules.rulesRight
    }
}