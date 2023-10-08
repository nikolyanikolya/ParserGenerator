package domain

data class Tree(val node: String, val value: Any? = null, val children: MutableList<Tree>) {
    constructor(node: String, value: Any? = null): this(node, value, mutableListOf())
}