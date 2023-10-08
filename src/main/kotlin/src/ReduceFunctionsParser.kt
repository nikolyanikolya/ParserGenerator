package src

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

object ReduceFunctionsParser {
    fun parseAndSaveToFile(input: List<String>, reduceFunctionsPath: Path) {
        Files.newBufferedWriter(reduceFunctionsPath).use { writer ->
            writer.write("import kotlin.reflect.KFunction")
            writer.newLine()
            val allFunctions = mutableListOf<String>()
            input.forEach {
                val res = it.split("@", limit = 3)
                if (res.size > 1) {
                    writer.write(res[1])
                    val functionName = res[1].trim().split(" ")[1]
                    allFunctions.add("::$functionName")
                    writer.newLine()
                }
            }
            writer.write(
                "val ${reduceFunctionsPath.nameWithoutExtension} : Array<KFunction<Any>> = arrayOf(${
                    allFunctions.joinToString(
                        ","
                    )
                })"
            )
            writer.newLine()
        }
    }
}