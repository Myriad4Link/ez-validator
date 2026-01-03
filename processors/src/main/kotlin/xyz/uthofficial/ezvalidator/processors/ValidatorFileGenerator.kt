package xyz.uthofficial.ezvalidator.processors

import arrow.core.Either
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import xyz.uthofficial.ezvalidator.states.errors.ValidationError
import xyz.uthofficial.ezvalidator.processors.handlers.toPoetParameterSpecs

class ValidatorFileGenerator(
    private val packageName: String
) {
    private val eitherClassName = Either::class.asClassName()
    private val errorType = ValidationError::class.asClassName()

    fun generate(
        classDeclaration: KSClassDeclaration,
        validationFunctions: List<FunSpec>
    ): FileSpec {
        val fileSpecBuilder = FileSpec.builder(
            packageName,
            "${classDeclaration.simpleName.asString()}Validator"
        )

        validationFunctions.forEach { fileSpecBuilder.addFunction(it) }

        val targetConstructor = classDeclaration.primaryConstructor!!
        val parameterSpecs = targetConstructor.parameters.toPoetParameterSpecs()
        val classType = classDeclaration.toClassName()
        val callArgs = parameterSpecs.map { CodeBlock.of("%N", it) }.joinToCode(", ")

        val createValidatedFun = FunSpec.builder("createValidated")
            .receiver(classType.nestedClass("Companion"))
            .returns(eitherClassName.parameterizedBy(LIST.parameterizedBy(errorType), classType))
            .addParameters(parameterSpecs)
            .addCode(
                CodeBlock.builder().apply {
                    addStatement("val errors = mutableListOf<%T>()", errorType)
                    validationFunctions.forEach { funSpec ->
                        val funName = funSpec.name
                        addStatement("val res_$funName = %N(%L)", funName, callArgs)
                        beginControlFlow("when (res_$funName)")
                        addStatement("is %T.Left -> errors.add(res_$funName.value)", eitherClassName)
                        addStatement("is %T.Right -> Unit", eitherClassName)
                        endControlFlow()
                    }
                    beginControlFlow("when")
                    addStatement("errors.isEmpty() -> return %T.Right(%T(%L))", eitherClassName, classType, callArgs)
                    addStatement("else -> return %T.Left(errors)", eitherClassName)
                    endControlFlow()
                }.build()
            )
            .build()

        fileSpecBuilder.addFunction(createValidatedFun)
        return fileSpecBuilder.build()
    }
}
