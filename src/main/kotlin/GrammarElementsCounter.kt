class GrammarElementsCounter(
    inputToNodeMapper: HashMap<String, Node>
) {

    private val grammarElementsCounter = inputToNodeMapper.keys.associateWith { 0L } as MutableMap
    operator fun get(key: String): Long? {
        return grammarElementsCounter[key]
    }

    fun put(key: String, value: Long) {
        grammarElementsCounter[key] = value
    }
}