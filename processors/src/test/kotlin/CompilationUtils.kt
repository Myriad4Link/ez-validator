import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
object CompilationUtils {
    fun compile(
        vararg source: SourceFile,
        options: Map<String, String> = emptyMap(),
        symbolProcessorProviders: List<SymbolProcessorProvider>
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = source.toList()
            useKsp2()
            this.symbolProcessorProviders = symbolProcessorProviders.toMutableList()
            kspProcessorOptions = options.toMutableMap()
            inheritClassPath = true
        }.compile()
    }
}
