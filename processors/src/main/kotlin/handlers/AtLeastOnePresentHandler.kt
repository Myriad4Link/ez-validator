package handlers

import arrow.core.Either
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName

class AtLeastOnePresentHandler : ValidationHandler {
    override val name: String = "AtLeastOnePresent"

    override fun canProcess(annotation: KSAnnotation): Boolean {
        return annotation.annotationType.resolve().declaration.qualifiedName?.asString() == "AtLeastOnePresent"
    }

    override fun process(classDeclaration: KSClassDeclaration): Result<FunSpec> {
        val className = classDeclaration.simpleName.asString()
        val constructor = classDeclaration.primaryConstructor
            ?: return Result.failure(IllegalArgumentException("Class $className must have a primary constructor to validate with"))

        val parameterSpecs = constructor.parameters.toPoetParameterSpecs()
        val optionParamNames = constructor.parameters.findArrowOptionParamNames()

        val conditionCode = when {
            optionParamNames.isNotEmpty() -> optionParamNames.joinToString(" || ") { "$it.isSome()" }
            else -> "true"
        }

        val constructorCallArgsBlock = parameterSpecs.map {
            CodeBlock.of("%N = %N", it, it)
        }.joinToCode(", ")

        val errorClassName = TooLessPresentedError::class.asClassName()
        val eitherClassName = Either::class.asClassName()

        return Result.success(
            FunSpec.builder("create$className")
                .returns(
                    eitherClassName.parameterizedBy(
                        errorClassName,
                        classDeclaration.toClassName()
                    )
                )
                .addParameters(parameterSpecs)
                .addStatement("val isAnyPresent = $conditionCode")
                .beginControlFlow("if (!isAnyPresent)")
                .addStatement(
                    "return %T.Left(%T(%L, %L))",
                    eitherClassName,
                    errorClassName,
                    1,
                    0
                )
                .endControlFlow()
                .addStatement(
                    "return %T.Right(%T(%L))",
                    eitherClassName,
                    classDeclaration.toClassName(),
                    constructorCallArgsBlock
                )
                .build()
        )
    }
}