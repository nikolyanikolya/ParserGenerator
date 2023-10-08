package utils

import java.lang.reflect.Modifier
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinProperty

object ReflectionUtils {
    fun obtainReduceFunctions(reduceFunctionsJavaClass: String,
                              reduceFunctionsFieldName: String): KProperty<Array<KFunction<Any>>>? {
        val selfRef = ReflectionUtils::obtainReduceFunctions
        val currentClass = selfRef.javaMethod!!.declaringClass
        val classDefiningFunctions = currentClass.classLoader.loadClass(reduceFunctionsJavaClass)
        val javaField = classDefiningFunctions.declaredFields.find { it.name == reduceFunctionsFieldName && Modifier.isStatic(it.modifiers) }
        return javaField?.kotlinProperty as KProperty<Array<KFunction<Any>>>?
    }

    fun obtainJavaClass(kotlinPath: Path): String =
        kotlinPath.nameWithoutExtension.capitalizeFirstChar() +
                kotlinPath.extension.capitalizeFirstChar()

    private fun String.capitalizeFirstChar() = this.replaceFirstChar { it.uppercaseChar() }
}