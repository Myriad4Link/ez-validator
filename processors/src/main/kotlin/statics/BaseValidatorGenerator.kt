package statics

import com.squareup.kotlinpoet.*

class BaseValidatorGenerator {
    fun generate(destinationPackageName: String, errorContextPackageName: String) {
        val typeT = TypeVariableName.Companion("T")
        val className = ClassName(destinationPackageName, "BaseValidator")
        val errorContextType = ClassName(errorContextPackageName, "ErrorContext")

        FileSpec.builder(className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.ABSTRACT)
                    .addTypeVariable(typeT)
                    .addFunction(
                        FunSpec.builder("whileFailingDo")
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
                            .addCode(
                                """
                                return %T(this.validationErrors).block()
                            """.trimIndent(),
                                errorContextType
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }
}