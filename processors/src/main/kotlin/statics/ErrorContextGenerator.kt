package statics

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import errors.ValidationError

class ErrorContextGenerator {
    fun generate(packageName: String) {
        val errorsList = List::class.asClassName().parameterizedBy(
            ClassName("errors", "ValidationError")
        )
        val typeE = TypeVariableName("E", ValidationError::class.asClassName()).copy(reified = true)

        FileSpec.builder(packageName, "ErrorContext")
            .addType(
                TypeSpec.classBuilder(ClassName(packageName, "ErrorContext"))
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(
                                "errors",
                                errorsList
                            ).build()
                    )
                    .addProperty(PropertySpec.builder("errors", errorsList).initializer("errors").build())
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
}