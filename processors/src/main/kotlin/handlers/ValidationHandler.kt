package handlers

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec

interface ValidationHandler {
    val name: String
        get() = this::class.simpleName!!.removeSuffix("Handler")

    fun canProcess(annotation: KSAnnotation): Boolean
    val functionName: String
        get() = "validate${name}"

    fun process(classDeclaration: KSClassDeclaration): Result<FunSpec>
}