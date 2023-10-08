import domain.ApplicationNeedRestartException
import src.AutomatonBuilder
import src.InputProcessor.process
import src.TokenizedGrammar
import src.Tokenizer.tokenize
import src.Visualizer
import src.WorkingStack
import java.nio.file.Files.newBufferedReader
import java.nio.file.Path.of

fun main(args: Array<String>) {
    val input = process(args.getOrElse(0) { "( ( 4 + 1 * 2 * 3 ) * 10 )" })
    val reduceFunctionsFileName = "src/main/kotlin/src/reduceFunctions.kt"

    val filePath = of(args.getOrElse(1) { "grammar.txt" })
    val rulesLines = newBufferedReader(filePath.toAbsolutePath()).use {
        it.readLines()
    }

    val tokenizedGrammar: TokenizedGrammar = runCatching { tokenize(rulesLines, reduceFunctionsFileName) }
        .getOrElse {
            when (it) {
                is ApplicationNeedRestartException -> {
                    println(it.message)
                    return
                }
                else -> throw it
            }
        }
    tokenizedGrammar.logRules(of("test.txt"))
    tokenizedGrammar.logTokens(of("tokens.txt"))
    val automaton = AutomatonBuilder(tokenizedGrammar).build()
    println("Automaton size: ${automaton.nka.size}")
    println(automaton.nka.keys.map { it.state })
    val tree =
        WorkingStack(
            input,
            automaton,
            tokenizedGrammar.nonTerminalToNode,
            tokenizedGrammar.terminalToNode,
            tokenizedGrammar.regexToNode
        ).process()

    val visualizer = Visualizer(tokenizedGrammar)
    visualizer.display(tree)
}