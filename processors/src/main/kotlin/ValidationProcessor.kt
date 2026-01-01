import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.writeTo
import handlers.AtLeastOnePresentHandler
import handlers.ValidationHandler

class ValidationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val fileGenerator: ValidatorFileGenerator,
    private val handlers: List<ValidationHandler> = listOf(AtLeastOnePresentHandler())
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getNewFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                it.annotations.any { ann ->
                    ann.annotationType.resolve().declaration.annotations.any { metaAnn ->
                        metaAnn.annotationType.resolve().declaration.qualifiedName?.asString() == Validator::class.qualifiedName
                    }
                }
            }

        if (!symbols.iterator().hasNext()) return emptyList()

        symbols.forEach { classDeclaration ->
            val containingFile = classDeclaration.containingFile ?: return@forEach

            val targetConstructor = classDeclaration.primaryConstructor
            when (targetConstructor) {
                null -> {
                    logger.error(
                        "Class ${classDeclaration.simpleName.asString()} must have a primary constructor",
                        classDeclaration
                    )
                    return@forEach
                }
            }

            val applicableHandlers = handlers.filter { handler ->
                classDeclaration.annotations.any { ann -> handler.canProcess(ann) }
            }

            val validationFunctions = applicableHandlers.mapNotNull { handler ->
                handler.process(classDeclaration).fold(
                    onSuccess = { it },
                    onFailure = {
                        logger.error(it.message ?: "Unknown error", classDeclaration)
                        null
                    }
                )
            }

            val fileSpec = fileGenerator.generate(classDeclaration, validationFunctions)
            fileSpec.writeTo(codeGenerator, Dependencies(false, containingFile))
        }
        return emptyList()
    }
}