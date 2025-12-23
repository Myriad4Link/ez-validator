import arrow.core.Either
import arrow.core.Option
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class ValidationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(AtLeastOnePresent::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        val sourceFiles = ArrayList<KSFile>()
        val fileSpecBuilder = FileSpec.builder(
            options["ez-validator.package"] ?: "xyz.uthofficial.ksp.ezvalidator",
            options["ez-validator.factoryName"] ?: "ValidatedClassFactory"
        )

        symbols.forEach {
            it.containingFile?.let { sourceFile -> sourceFiles.add(sourceFile) }
            createFactorySpec(it)
                .onFailure { e -> logger.error(e.message!!, it) }
                .onSuccess(fileSpecBuilder::addFunction)
        }

        fileSpecBuilder.build()
            .writeTo(
                codeGenerator,
                Dependencies(true, *sourceFiles.toTypedArray())
            )

        return emptyList()
    }

    private fun createFactorySpec(classDeclaration: KSClassDeclaration): Result<FunSpec> {
        val className = classDeclaration.simpleName.asString()

        val constructor = classDeclaration.primaryConstructor

        when {
            constructor == null -> {
                return Result.failure(IllegalArgumentException("Class $className must have a primary constructor to validate with"))
            }
        }

        val parameterSpecs = constructor.parameters.toPoetParameterSpecs()
        val optionParamNames = constructor.parameters.findArrowOptionParamNames()

        val conditionCode = when {
            optionParamNames.isNotEmpty() -> optionParamNames.joinToString("||") { "${it}.isSome()" }
            else -> "true"
        }

        val constructorCallArgsBlock = parameterSpecs.map {
            CodeBlock.of("""%N = %N""", it, it)
        }.joinToCode(", ")

        return Result.success(
            FunSpec.builder("create$className")
                // Since it is a factory function where failure is expected, we return EITHER a validation error or the
                // successfully created one.
                .returns(
                    Either::class.asClassName()
                        .parameterizedBy(
                            TooLessPresentedError::class.asClassName(),
                            classDeclaration.toClassName()
                        )
                )
                // Add the parameters extracted from the original one.
                .addParameters(parameterSpecs)
                .addStatement("val isAnyPresent = $conditionCode")
                .beginControlFlow("if (!isAnySet)")
                // Expected (at least) 1, found 0.
                .addStatement(
                    """return %T.Left(%T(%L, %L))""",
                    Either::class.asClassName(),
                    TooLessPresentedError::class.asClassName(),
                    1,
                    0
                )
                .endControlFlow()
                .addStatement(
                    """return %T.Right(%T(%L))""",
                    Either::class.asClassName(),
                    classDeclaration.toClassName(),
                    constructorCallArgsBlock
                )
                .build()
        )
    }

    private fun List<KSValueParameter>.toPoetParameterSpecs(): List<ParameterSpec> = this.map {
        val name = it.name!!.asString()
        val type = it.type.resolve().toTypeName()
        ParameterSpec.builder(name, type).build()
    }

    private fun List<KSValueParameter>.findArrowOptionParamNames(): List<String> = this.filter {
        it.type.resolve().declaration.qualifiedName?.asString() == Option::class.qualifiedName!!
    }
        .map { it.name!!.asString() }
}