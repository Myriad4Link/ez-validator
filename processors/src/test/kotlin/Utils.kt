import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FunSpec
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import handlers.AtLeastOnePresentHandler
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

class ProcessorResultRef {
    var value: Result<FunSpec>? = null
}

fun createHandlerSpecificProcessorProvider(
    handler: AtLeastOnePresentHandler
): Pair<SymbolProcessorProvider, ProcessorResultRef> {

    val holder = ProcessorResultRef()

    val provider = SymbolProcessorProvider {
        object: SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                holder.value = handler.process(
                    resolver.getClassDeclarationByName(
                        resolver.getKSNameFromString("xyz.uthofficial.tests.Test")
                    )!!
                )
                return emptyList()
            }
        }
    }

    return provider to holder
}

@OptIn(ExperimentalCompilerApi::class)
fun createCompilation(
    sourceFiles: List<SourceFile>,
    processorProviders: MutableList<SymbolProcessorProvider>
) = KotlinCompilation().apply {
    useKsp2()
    symbolProcessorProviders = processorProviders
    sources = sourceFiles
    inheritClassPath = true
}