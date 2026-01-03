package xyz.uthofficial.ezvalidator.processors.handlers

import xyz.uthofficial.ezvalidator.states.Ok
import arrow.core.Either
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import xyz.uthofficial.ezvalidator.states.errors.TooLessPresentedError

class AtLeastOnePresentHandler : ValidationHandler {
    override fun canProcess(annotation: KSAnnotation): Boolean =
        annotation.annotationType.resolve().declaration.simpleName.asString() == name

    override fun process(classDeclaration: KSClassDeclaration): Result<FunSpec> {
        val targetConstructor = classDeclaration.primaryConstructor
        when (targetConstructor) {
            null -> return Result.failure(IllegalArgumentException("Class ${classDeclaration.simpleName.asString()} must have a primary constructor"))
        }

        val parameterSpecs = targetConstructor.parameters.toPoetParameterSpecs()
        val optionParamNames = targetConstructor.parameters.findArrowOptionParamNames()

        val conditionCode = when {
            optionParamNames.isNotEmpty() -> optionParamNames.joinToString(" || ") { "$it.isSome()" }
            else -> "true"
        }

        val errorClassName = TooLessPresentedError::class.asClassName()
        val eitherClassName = Either::class.asClassName()
        val okClassName = Ok::class.asClassName()

        return Result.success(
            FunSpec.builder(functionName)
                .addModifiers(KModifier.PRIVATE)
                .returns(eitherClassName.parameterizedBy(errorClassName, okClassName))
                .addParameters(parameterSpecs)
                .addStatement("val isAnyPresent = $conditionCode")
                .beginControlFlow("when")
                .addStatement("!isAnyPresent -> return %T.Left(%T(%L, %L))", eitherClassName, errorClassName, 1, 0)
                .addStatement("else -> return %T.Right(%T)", eitherClassName, okClassName)
                .endControlFlow()
                .build()
        )
    }
}
