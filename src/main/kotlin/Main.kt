import InputProcessor.process
import Tokenizer.tokenize
import java.nio.file.Files.newBufferedReader
import java.nio.file.Path.of

fun main(args: Array<String>) {
    val input = process("'n' '+' 'n' '*' 'n'")
    val filePath = of(args.getOrElse(0) { "grammar.txt" })
    val rulesLines = newBufferedReader(filePath.toAbsolutePath()).use {
        it.readLines()
    }

    val tokenizedGrammar = tokenize(rulesLines)
    tokenizedGrammar.writeRules(of("test.txt"))
    tokenizedGrammar.writeTokens(of("tokens.txt"))
    val automaton = AutomatonBuilder(tokenizedGrammar).build()
    WorkingStack(input, automaton, tokenizedGrammar.inputToNode).process()

}