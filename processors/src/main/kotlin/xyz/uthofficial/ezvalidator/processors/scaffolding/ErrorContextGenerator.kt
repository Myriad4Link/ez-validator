package xyz.uthofficial.ezvalidator.processors.scaffolding

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import xyz.uthofficial.ezvalidator.states.errors.ValidationError

class ErrorContextGenerator {
    fun generate(packageName: String) {
        val errorsList = List::class.asClassName().parameterizedBy(
            ClassName("xyz/uthofficial/ezvalidator/states/errors", "ValidationError")
        )
        val typeE = TypeVariableName("E", ValidationError::class.asClassName()).copy(reified = true)

        FileSpec.builder(packageName, "ErrorContext")
            .addType(
                TypeSpec.classBuilder(ClassName(packageName, "ErrorContext"))
                    .primaryConstructor(generatePrimaryConstructor(errorsList))
                    .addProperty(generateErrorsProp(errorsList))
                    .addFunction(
                        FunSpec.builder("on")
                            .addTypeVariable(typeE)
                            .addParameter(
                                "block", LambdaTypeName.get(parameters = arrayOf(typeE), returnType = UNIT)
                            )
                            .addModifiers(KModifier.INLINE)
                            .addCode(
                                """
                                errors.filterIsInstance<%T>().forEach { block(it) }
                            """.trimIndent(),
                                typeE
                            )
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("has")
                            .addTypeVariable(typeE)
                            .addModifiers(KModifier.INLINE)
                            .returns(BOOLEAN)
                            .addCode(
                                """
                                return errors.any { it is %T }
                            """.trimIndent(),
                                typeE
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun generateErrorsProp(errorsList: ParameterizedTypeName): PropertySpec =
        PropertySpec.builder("xyz/uthofficial/ezvalidator/states/errors", errorsList).initializer("xyz/uthofficial/ezvalidator/states/errors").build()

    private fun generatePrimaryConstructor(errorsList: ParameterizedTypeName): FunSpec = FunSpec.constructorBuilder()
        .addParameter(
            "xyz/uthofficial/ezvalidator/states/errors",
            errorsList
        ).build()
}