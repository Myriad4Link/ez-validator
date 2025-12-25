import arrow.core.Either
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import errors.ValidationError
import handlers.AtLeastOnePresentHandler
import handlers.ValidationHandler
import handlers.toPoetParameterSpecs

class ValidationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
    private val handlers: List<ValidationHandler> = listOf(AtLeastOnePresentHandler())
) : SymbolProcessor {
    private val eitherClassName = Either::class.asClassName()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Validator::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { annotation ->
                annotation.qualifiedName?.asString()?.let { resolver.getSymbolsWithAnnotation(it) } ?: emptySequence()
            }
            .filterIsInstance<KSClassDeclaration>()
            .distinct()

        when {
            !symbols.iterator().hasNext() -> return emptyList()
        }

        symbols.forEach { classDeclaration ->
            val containingFile = classDeclaration.containingFile ?: return@forEach

            val applicableHandlers = handlers.filter { handler ->
                classDeclaration.annotations.any { ann -> handler.canProcess(ann) }
            }

            val targetConstructor = classDeclaration.primaryConstructor
            when (targetConstructor) {
                null -> {
                    logger.error("Class ${classDeclaration.simpleName.asString()} must have a primary constructor", classDeclaration)
                    return@forEach
                }
            }

            val fileSpecBuilder = FileSpec.builder(
                options["ez-validator.package"] ?: "xyz.uthofficial.ezvalidator.ksp",
                "${classDeclaration.simpleName.asString()}Validator"
            )
            val generatedFuns = mutableListOf<String>()

            applicableHandlers.forEach { handler ->
                handler.process(classDeclaration).fold(
                    onSuccess = { funSpec ->
                        fileSpecBuilder.addFunction(funSpec)
                        generatedFuns.add(funSpec.name)
                    },
                    onFailure = { logger.error(it.message ?: "Unknown error", classDeclaration) }
                )
            }

            val parameterSpecs = targetConstructor.parameters.toPoetParameterSpecs()
            val classType = classDeclaration.toClassName()
            val callArgs = parameterSpecs.map { CodeBlock.of("%N", it) }.joinToCode(", ")
            val errorType = ValidationError::class.asClassName()

            fileSpecBuilder.addFunction(
                FunSpec.builder("createValidated")
                    .receiver(classType.nestedClass("Companion"))
                    .returns(eitherClassName.parameterizedBy(LIST.parameterizedBy(errorType), classType))
                    .addParameters(parameterSpecs)
                    .addCode(
                        CodeBlock.builder().apply {
                            addStatement("val errors = mutableListOf<%T>()", errorType)
                            generatedFuns.forEach {
                                addStatement("val res_$it = %N(%L)", it, callArgs)
                                beginControlFlow("when (res_$it)")
                                addStatement("is %T.Left -> errors.add(res_$it.value)", eitherClassName)
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
            )

            fileSpecBuilder.build().writeTo(codeGenerator, Dependencies(false, containingFile))
        }
        return emptyList()
    }
}