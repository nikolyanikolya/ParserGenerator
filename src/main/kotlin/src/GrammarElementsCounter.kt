package src

class GrammarElementsCounter(
    grammar: TokenizedGrammar,
) {

    private val grammarElementsCounter: MutableMap<String, Long> =
        (grammar.nonTerminalToNode.keys + grammar.terminalToNode.keys).associateWith { 0L } as MutableMap
    private val regexElementsCounter: MutableMap<Regex, Long> = grammar.regexToNode.keys.associateWith { 0L } as MutableMap
    operator fun get(key: String): Long {
        return grammarElementsCounter[key] ?: regexElementsCounter.entries.first {
            (regex, _) ->
            key.matches(regex)
        }.value
    }

    fun put(key: String, value: Long) {
        if (grammarElementsCounter.containsKey(key)) {
            grammarElementsCounter[key] = value
        } else {
            val (regex, _) = regexElementsCounter.entries.first {
                key.matches(it.key)
            }
            regexElementsCounter[regex] = value
        }
    }
}