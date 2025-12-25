import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FunSpec
import com.tschuchort.compiletesting.*
import handlers.ValidationHandler
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

fun createTestSourceFile(filename: String, content: String): SourceFile =
    SourceFile.kotlin(filename, content, true)

fun createTestSourceFile(filename: String, vararg parameters: String) =
    createTestSourceFile(
        filename, """
        package xyz.uthofficial.tests
        import arrow.core.Option
        
        data class $filename(${parameters.joinToString(", ")})
    """.trimIndent()
    )

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

fun ValidationHandler.testWith(vararg sources: SourceFile, targetClass: String = "xyz.uthofficial.tests.Test") =
    HandlerCompilationContext(sources.toList(), targetClass).compileWith(this)

fun String.asSourceFile(name: String = "Test.kt") = SourceFile.kotlin(name, this, true)
