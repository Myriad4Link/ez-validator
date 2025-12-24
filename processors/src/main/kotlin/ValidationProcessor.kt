import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import handlers.AtLeastOnePresentHandler
import handlers.ValidationHandler

class ValidationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
    private val handlers: List<ValidationHandler> = listOf(AtLeastOnePresentHandler())
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("AtLeastOnePresent")
            .filterIsInstance<KSClassDeclaration>()

        when {
            (symbols.iterator().hasNext().not()) -> return emptyList()
        }

        val sourceFiles = ArrayList<KSFile>()
        val fileSpecBuilder = FileSpec.builder(
            options["ez-validator.package"] ?: "xyz.uthofficial.ksp.ezvalidator",
            options["ez-validator.factoryName"] ?: "ValidatedClassFactory"
        )

        symbols.forEach { classDeclaration ->
            classDeclaration.containingFile?.let(sourceFiles::add)

            val annotation = classDeclaration.annotations.firstOrNull { ann ->
                handlers.any { it.canProcess(ann) }
            }

            val handler = annotation?.let { ann ->
                handlers.firstOrNull { it.canProcess(ann) }
            }

            handler?.process(classDeclaration)
                ?.onFailure { logger.error(it.message ?: "Unknown error", classDeclaration) }
                ?.onSuccess(fileSpecBuilder::addFunction)
        }

        fileSpecBuilder.build()
            .writeTo(
                codeGenerator,
                Dependencies(true, *sourceFiles.toTypedArray())
            )

        return emptyList()
    }
}