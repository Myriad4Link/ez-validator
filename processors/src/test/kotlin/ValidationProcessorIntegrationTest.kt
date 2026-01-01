import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import com.tschuchort.compiletesting.KotlinCompilation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class ValidationProcessorIntegrationTest : FunSpec({
    test("should generate factory correctly") {
        val source = """
            package xyz.uthofficial.tests
            
            import arrow.core.Option
            import AtLeastOnePresent
            
            @AtLeastOnePresent
            data class Test(val opt1: Option<String>, val opt2: Option<Int>, val opt3: Option<Double>) {
                companion object
            }
       """.trimIndent().asSourceFile()

        val consumer = """
            package xyz.uthofficial.tests
            
            import xyz.uthofficial.tests.Test
            import xyz.uthofficial.ezvalidator.ksp.createValidated
            import arrow.core.None
            
            fun main() = Test.createValidated(None, None, None)
        """.trimIndent().asSourceFile("Main.kt")

        val compilationResult = compileWithProcessor(source, consumer)

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    test("should fail compilation on invalid usage") {
        val source = """
            package xyz.uthofficial.tests
            
            import AtLeastOnePresent
            
            @AtLeastOnePresent
            interface TestInterface {
                fun something()
            }
        """.trimIndent().asSourceFile()

        val compilationResult = compileWithProcessor(source)

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        compilationResult.messages shouldContain "Class TestInterface must have a primary constructor"
    }

    test("should create instance via factory when valid") {
        val source = """
            package xyz.uthofficial.tests
            
            import arrow.core.Option
            import AtLeastOnePresent
            
            @AtLeastOnePresent
            data class Test(val opt1: Option<String>, val opt2: Option<Int>) {
                companion object
            }
        """.trimIndent().asSourceFile()

        val compilationResult = compileWithProcessor(source)

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val result =
            compilationResult.classLoader.loadClass("xyz.uthofficial.ezvalidator.ksp.TestValidatorKt").getMethod(
                "createValidated",
                compilationResult.classLoader.loadClass($$"xyz.uthofficial.tests.Test$Companion"),
                arrow.core.Option::class.java,
                arrow.core.Option::class.java
            ).invoke(
                null,
                compilationResult.classLoader.loadClass("xyz.uthofficial.tests.Test")
                    .getDeclaredField("Companion")
                    .get(null),
                Some("valid"),
                None
            )

        result.shouldBeInstanceOf<Either.Right<*>>()
        result.value shouldNotBeNull {
            javaClass.name shouldBe "xyz.uthofficial.tests.Test"

            val opt1 = javaClass.getDeclaredMethod("getOpt1").invoke(this)
            val opt2 = javaClass.getDeclaredMethod("getOpt2").invoke(this)

            opt1 shouldBe Some("valid")
            opt2 shouldBe None
        }
    }

    test("should return error via factory when not valid") {
        val source = """
            package xyz.uthofficial.tests
            
            import arrow.core.Option
            import AtLeastOnePresent
            
            @AtLeastOnePresent
            data class Test(val opt1: Option<String>, val opt2: Option<Int>) {
                companion object
            }
        """.trimIndent().asSourceFile()

        val compilationResult = compileWithProcessor(source)

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val result =
            compilationResult.classLoader.loadClass("xyz.uthofficial.ezvalidator.ksp.TestValidatorKt").getMethod(
                "createValidated",
                compilationResult.classLoader.loadClass($$"xyz.uthofficial.tests.Test$Companion"),
                arrow.core.Option::class.java,
                arrow.core.Option::class.java
            ).invoke(
                null,
                compilationResult.classLoader.loadClass("xyz.uthofficial.tests.Test")
                    .getDeclaredField("Companion")
                    .get(null),
                None,
                None
            )

        result.shouldBeInstanceOf<Either.Left<List<*>>>()
        val errors = result.value

        errors shouldHaveSize 1
        val error = errors.first()!!
        error.javaClass.name shouldBe "errors.TooLessPresentedError"

        error.javaClass.getDeclaredMethod("getRequired").invoke(error) shouldBe 1
        error.javaClass.getDeclaredMethod("getPresented").invoke(error) shouldBe 0
    }
})