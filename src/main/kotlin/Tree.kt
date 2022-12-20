data class Tree(val node: String, val children: MutableList<Tree>) {
    constructor(node: String): this(node, mutableListOf())
}