import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.tschuchort.compiletesting.*
import xyz.uthofficial.ezvalidator.processors.handlers.ValidationHandler
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import xyz.uthofficial.ezvalidator.processors.ValidationProcessorProvider
import xyz.uthofficial.ezvalidator.processors.ValidatorFileGenerator

fun createTestSourceFile(filename: String, content: String): SourceFile =
    SourceFile.kotlin(filename, content, true)

@OptIn(ExperimentalCompilerApi::class)
class HandlerCompilationContext(
    val sourceFiles: List<SourceFile>,
    val targetClassName: String = "xyz.uthofficial.tests.Test"
) {
    private var handlerResult: Result<FunSpec>? = null

    fun compileWith(handler: ValidationHandler): HandlerCompilationResult {
        val provider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    resolver.getClassDeclarationByName(resolver.getKSNameFromString(targetClassName))?.let {
                        handlerResult = handler.process(it)
                    }
                    return emptyList()
                }
            }
        }

        val compilation = KotlinCompilation().apply {
            useKsp2()
            symbolProcessorProviders = mutableListOf(provider)
            sources = sourceFiles
            inheritClassPath = true
        }

        return HandlerCompilationResult(compilation.compile(), handlerResult)
    }
}

@OptIn(ExperimentalCompilerApi::class)
class HandlerCompilationResult(
    val compilationResult: JvmCompilationResult,
    val processorResult: Result<FunSpec>?
)

@OptIn(ExperimentalCompilerApi::class)
class GeneratorCompilationContext(
    val sourceFiles: List<SourceFile>,
    val targetClassName: String = "xyz.uthofficial.ezvalidator.tests.Test"
) {
    private var generatedFileSpec: FileSpec? = null

    fun compileWith(generator: ValidatorFileGenerator, funSpecs: List<FunSpec>): GeneratorCompilationResult {
        val provider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    resolver.getClassDeclarationByName(resolver.getKSNameFromString(targetClassName))?.let {
                        generatedFileSpec = generator.generate(it, funSpecs)
                    }
                    return emptyList()
                }
            }
        }

        val compilation = KotlinCompilation().apply {
            useKsp2()
            symbolProcessorProviders = mutableListOf(provider)
            sources = sourceFiles
            inheritClassPath = true
        }

        return GeneratorCompilationResult(compilation.compile(), generatedFileSpec)
    }
}

@OptIn(ExperimentalCompilerApi::class)
class GeneratorCompilationResult(
    val compilationResult: JvmCompilationResult,
    val generatedFileSpec: FileSpec?
)

@OptIn(ExperimentalCompilerApi::class)
fun ValidationHandler.testWith(vararg sources: SourceFile, targetClass: String = "xyz.uthofficial.tests.Test") =
    HandlerCompilationContext(sources.toList(), targetClass).compileWith(this)

@OptIn(ExperimentalCompilerApi::class)
fun ValidatorFileGenerator.testWith(
    funSpecs: List<FunSpec>,
    vararg sources: SourceFile,
    targetClass: String = "xyz.uthofficial.ezvalidator.tests.Test"
) = GeneratorCompilationContext(sources.toList(), targetClass).compileWith(this, funSpecs)

fun String.asSourceFile(name: String = "Test.kt") = SourceFile.kotlin(name, this, true)

@OptIn(ExperimentalCompilerApi::class)
fun compileWithProcessor(
    vararg sources: SourceFile,
    providers: List<SymbolProcessorProvider> = listOf(ValidationProcessorProvider())
): JvmCompilationResult =
    KotlinCompilation().apply {
        useKsp2()
        this.sources = sources.toList()
        inheritClassPath = true
        symbolProcessorProviders = providers.toMutableList()
    }.compile()
