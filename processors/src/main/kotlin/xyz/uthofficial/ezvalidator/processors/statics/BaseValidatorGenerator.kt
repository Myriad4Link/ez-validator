package xyz.uthofficial.ezvalidator.processors.statics

import arrow.core.None
import arrow.core.Option
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class BaseValidatorGenerator {
    fun generate(destinationPackageName: String, errorContextPackageName: String) {
        val typeT = TypeVariableName.Companion("T")
        val className = ClassName(destinationPackageName, "BaseValidator")
        val errorContextType = ClassName(errorContextPackageName, "ErrorContext")

        buildBaseValidatorClass(className, typeT, errorContextType)
    }

    private fun buildBaseValidatorClass(
        className: ClassName,
        typeT: TypeVariableName,
        errorContextType: ClassName
    ) {
        FileSpec.builder(className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.ABSTRACT)
                    .addTypeVariable(typeT)
                    .addProperty(buildRemediationStrategyProp(errorContextType, typeT))
                    .addFunction(buildWhileFailingDoFunc(typeT, errorContextType, className))
                    .addFunction(
                        buildCreateValidatedFunc(typeT)
                    )
                    .build()
            )
            .build()
    }

    private fun buildCreateValidatedFunc(typeT: TypeVariableName): FunSpec = FunSpec.builder("createValidated")
        .addModifiers(KModifier.ABSTRACT)
        .returns(typeT)
        .build()

    private fun buildRemediationStrategyProp(
        errorContextType: ClassName,
        typeT: TypeVariableName
    ): PropertySpec = PropertySpec.builder(
        "remediationStrategy",
        Option::class.asClassName()
            .parameterizedBy(LambdaTypeName.get(receiver = errorContextType, returnType = typeT))
    )
        .addModifiers(KModifier.PRIVATE)
        .mutable(true)
        .initializer("%T()", None::class.asClassName())
        .build()

    private fun buildWhileFailingDoFunc(
        typeT: TypeVariableName,
        errorContextType: ClassName,
        className: ClassName
    ): FunSpec = FunSpec.builder("whileFailingDo")
        .addParameter(
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(
                    returnType = typeT,
                    receiver = errorContextType
                )
            ).build()
        )
        .returns(className)
        .addStatement("this.remediationStrategy = %T.Some(block)", Option::class.asClassName())
        .addStatement("return this")
        .build()
}