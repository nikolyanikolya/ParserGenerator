import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.implementations.MultiGraph
import java.util.function.Consumer

class Visualizer(
    grammar: TokenizedGrammar,
) {

    private val grammarElementsCounter: GrammarElementsCounter = GrammarElementsCounter(grammar)
    fun display(tree: Tree) {
        System.setProperty("org.graphstream.ui", "javafx")
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")
        val graph: Graph = MultiGraph("Tree", false, false)
        try {
            graphTraversal(tree, graph)
            graph.display(true)
        } catch (e: Exception) {
            System.err.println(e.message)
        }
    }

    private fun graphTraversal(
        tree: Tree,
        graph: Graph,
    ): org.graphstream.graph.Node {
        var treeNodeName = tree.node
        var valueByKey: Long = grammarElementsCounter[tree.node]
        treeNodeName += valueByKey
        valueByKey += 1
        grammarElementsCounter.put(tree.node, valueByKey)
        val treeNode = graph.addNode(treeNodeName) as org.graphstream.graph.Node
        treeNode.addAttribute(
            "ui.style", "shape:circle;" +
                    "fill-color: green;size: 30px; text-alignment: center; text-size: 15px;"
        )
        treeNode.addAttribute("ui.label", tree.node)
        val finalTreeNodeName = treeNodeName
        tree.children.forEach(
            Consumer { child: Tree ->
                val childNode = graphTraversal(child, graph)
                graph.addEdge(finalTreeNodeName + " -> " + childNode.id, treeNode, childNode) as Edge
            }
        )
        return treeNode
    }
}
