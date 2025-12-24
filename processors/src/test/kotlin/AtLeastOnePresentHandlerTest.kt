import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import handlers.AtLeastOnePresentHandler
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@Suppress("RedundantInterpolationPrefix")
@OptIn(ExperimentalCompilerApi::class)
class AtLeastOnePresentHandlerTest {

    @Test
    fun `should generate correct FunSpec for class with options`() {
        val source = SourceFile.kotlin(
            "TestClass.kt", $$"""
                import AtLeastOnePresent
                import arrow.core.Option
                @AtLeastOnePresent
                data class TestClass(val opt1: Option<String>, val opt2: Option<Int>)
            """.trimIndent()
        )

        var capturedFunSpec: FunSpec? = null
        val handler = AtLeastOnePresentHandler()

        val provider = SymbolProcessorProvider { _ ->
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    resolver.getSymbolsWithAnnotation("AtLeastOnePresent")
                        .filterIsInstance<KSClassDeclaration>()
                        .forEach {
                            capturedFunSpec = handler.process(it).getOrNull()
                        }
                    return emptyList()
                }
            }
        }

        val result = CompilationUtils.compile(source, symbolProcessorProviders = listOf(provider))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        assertNotNull(capturedFunSpec)
        capturedFunSpec?.let {
            assertEquals("createTestClass", it.name)
            assertEquals(2, it.parameters.size)
            assertTrue(it.body.toString().contains("val isAnyPresent = opt1.isSome() || opt2.isSome()"))
        }
    }

    @Test
    fun `should handle class with no options`() {
        val source = SourceFile.kotlin(
            "NoOptionClass.kt", $$"""
                import AtLeastOnePresent
                @AtLeastOnePresent
                data class NoOptionClass(val name: String)
            """.trimIndent()
        )

        var capturedFunSpec: FunSpec? = null
        val handler = AtLeastOnePresentHandler()

        val provider = SymbolProcessorProvider { _ ->
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    resolver.getSymbolsWithAnnotation("AtLeastOnePresent")
                        .filterIsInstance<KSClassDeclaration>()
                        .forEach {
                            capturedFunSpec = handler.process(it).getOrNull()
                        }
                    return emptyList()
                }
            }
        }

        CompilationUtils.compile(source, symbolProcessorProviders = listOf(provider))

        assertNotNull(capturedFunSpec)
        capturedFunSpec?.let {
            assertTrue(it.body.toString().contains("val isAnyPresent = true"))
        }
    }
}
