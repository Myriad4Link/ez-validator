import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.tschuchort.compiletesting.*
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@Suppress("RedundantInterpolationPrefix")
@OptIn(ExperimentalCompilerApi::class)
class ValidationIntegrationTest {
    private fun compile(vararg source: SourceFile, options: Map<String, String> = emptyMap()): JvmCompilationResult {
        return CompilationUtils.compile(
            *source,
            options = options,
            symbolProcessorProviders = listOf(ValidationProcessorProvider())
        )
    }

    @Test
    fun `should succeed when at least one option is present`() {
        val source = SourceFile.kotlin(
            "TestClass.kt", $$"""
                package xyz.uthofficial.ezvalidator.tests
                import AtLeastOnePresent
                import arrow.core.Option

                @AtLeastOnePresent
                data class TestClass(val opt1: Option<String>, val opt2: Option<Int>)
            """.trimIndent()
        )
        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val factoryClass = result.classLoader.loadClass("xyz.uthofficial.ksp.ezvalidator.ValidatedClassFactoryKt")
        val createMethod = factoryClass.getDeclaredMethod("createTestClass", Option::class.java, Option::class.java)

        // Case 1: Both Some
        val res1 = createMethod.invoke(null, Some("hello"), Some(123)) as Either<*, *>
        assertTrue(res1.isRight())

        // Case 2: One Some
        val res2 = createMethod.invoke(null, Some("hello"), None) as Either<*, *>
        assertTrue(res2.isRight())

        // Case 3: All None
        val res3 = createMethod.invoke(null, None, None) as Either<*, *>
        assertTrue(res3.isLeft())
        val error = res3.swap().getOrNull() as TooLessPresentedError
        assertEquals(1, error.required)
        assertEquals(0, error.presented)
    }

    @Test
    fun `should handle mixed parameters correctly`() {
        val source = SourceFile.kotlin(
            "MixedClass.kt", $$"""
                package xyz.uthofficial.ezvalidator.tests
                import AtLeastOnePresent
                import arrow.core.Option

                @AtLeastOnePresent
                data class MixedClass(val name: String, val age: Int, val email: Option<String>)
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val factoryClass = result.classLoader.loadClass("xyz.uthofficial.ksp.ezvalidator.ValidatedClassFactoryKt")
        val createMethod =
            factoryClass.getDeclaredMethod("createMixedClass", String::class.java, Int::class.java, Option::class.java)

        // Case 1: Option is Some
        val res1 = createMethod.invoke(null, "John", 30, Some("john@example.com")) as Either<*, *>
        assertTrue(res1.isRight())

        // Case 2: Option is None
        val res2 = createMethod.invoke(null, "John", 30, None) as Either<*, *>
        assertTrue(res2.isLeft())
    }

    @Test
    fun `should succeed always when no options are present`() {
        val source = SourceFile.kotlin(
            "NoOptionClass.kt", $$"""
                package xyz.uthofficial.ezvalidator.tests
                import AtLeastOnePresent
                import arrow.core.Option

                @AtLeastOnePresent
                data class NoOptionClass(val name: String, val age: Int)
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val factoryClass = result.classLoader.loadClass("xyz.uthofficial.ksp.ezvalidator.ValidatedClassFactoryKt")
        val createMethod = factoryClass.getDeclaredMethod("createNoOptionClass", String::class.java, Int::class.java)

        val res = createMethod.invoke(null, "John", 30) as Either<*, *>
        assertTrue(res.isRight())
    }

    @Test
    fun `should respect custom package and factory name options`() {
        val source = SourceFile.kotlin(
            "CustomClass.kt", $$"""
                package xyz.uthofficial.ezvalidator.tests
                import AtLeastOnePresent
                import arrow.core.Option

                @AtLeastOnePresent
                data class CustomClass(val opt: Option<String>)
            """.trimIndent()
        )

        val options = mapOf(
            "ez-validator.package" to "com.example.custom",
            "ez-validator.factoryName" to "MyCustomFactory"
        )

        val result = compile(source, options = options)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val factoryClass = result.classLoader.loadClass("com.example.custom.MyCustomFactoryKt")
        assertNotNull(factoryClass)
        val createMethod = factoryClass.getDeclaredMethod("createCustomClass", Option::class.java)
        assertNotNull(createMethod)
    }
}
