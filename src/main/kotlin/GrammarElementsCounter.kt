class GrammarElementsCounter {
    private val grammarElementsCounter = mutableMapOf(
            "S" to 0L,
            "E'" to 0L,
            "E" to 0L,
            "T" to 0L,
            "T'" to 0L,
            "F" to 0L,
            "\'e\'" to 0L,
            "\'(\'" to 0L,
            "\')\'" to 0L,
            "\'*\'" to 0L,
            "\'$\'" to 0L,
            "\'+\'" to 0L,
            "\'n\'" to 0L,
        )

    operator fun get(key: String): Long? {
        return grammarElementsCounter[key]
    }

    fun put(key: String, value: Long) {
        grammarElementsCounter[key] = value
    }
}