package handlers

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec

interface ValidationHandler {
    val name: String
    fun canProcess(annotation: KSAnnotation): Boolean
    val functionName: String
        get() = "validate${name}Validation"

    fun process(classDeclaration: KSClassDeclaration): Result<FunSpec>
}