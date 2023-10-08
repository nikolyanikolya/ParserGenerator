package src

object InputProcessor{
    fun process(input: String) = ArrayDeque(input.trim().split(" ").map { it.trim() })

}