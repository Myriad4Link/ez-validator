package handlers

import Ok
import arrow.core.Either
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.tschuchort.compiletesting.*
import asSourceFile
import testWith
import errors.TooLessPresentedError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class AtLeastOnePresentHandlerTest : FunSpec({
    val sourceFile = """
        package xyz.uthofficial.tests
        import arrow.core.Option
        
        data class Test(val opt1: Option<String>, val opt2: Option<Int>, val opt3: Option<Double>)
    """.trimIndent().asSourceFile()

    val handler = AtLeastOnePresentHandler()
    val compilation = handler.testWith(sourceFile)
    val processorResult = compilation.processorResult

    test("should compile without errors") {
        compilation.compilationResult.exitCode shouldBeEqual KotlinCompilation.ExitCode.OK
    }

    test("should generate correct validation function signature") {
        processorResult.shouldNotBeNull().getOrThrow().apply {
            name shouldBe "validateAtLeastOnePresent"
            parameters.size shouldBe 3
            returnType shouldBeEqual Either::class.asClassName()
                .parameterizedBy(TooLessPresentedError::class.asClassName(), Ok::class.asClassName())
            modifiers.shouldContainOnly(KModifier.PRIVATE)
        }
    }

    test("should generate correct condition code when there are no Option parameters") {
        val source = """
            package xyz.uthofficial.tests
            data class Test(val str: String, val num: Int)
        """.trimIndent().asSourceFile()

        val result = handler.testWith(source, targetClass = "xyz.uthofficial.tests.Test")
        result.processorResult shouldNotBeNull {
            getOrThrow().toString() shouldContain "val isAnyPresent = true"
        }
    }

    test("should generate correct condition code when there are mixed Option parameters") {
        val source = """
            package xyz.uthofficial.tests
            import arrow.core.Option
            
            data class Test(val p0: Option<String>, val p1: Double, val p2: Option<Int>)
        """.trimIndent().asSourceFile()

        val result = handler.testWith(source, targetClass = "xyz.uthofficial.tests.Test")

        result.processorResult.shouldNotBeNull {
            getOrThrow().toString() shouldContain "val isAnyPresent = p0.isSome() || p2.isSome()"
        }
    }
})