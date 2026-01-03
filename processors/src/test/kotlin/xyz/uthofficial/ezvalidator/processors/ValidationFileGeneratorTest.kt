package xyz.uthofficial.ezvalidator.processors

import asSourceFile
import com.squareup.kotlinpoet.ParameterSpec
import com.tschuchort.compiletesting.KotlinCompilation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import testWith

@OptIn(ExperimentalCompilerApi::class)
class ValidationFileGeneratorTest : FunSpec({
    test("should generate correct file spec") {
        val source = """
            package xyz.uthofficial.ezvalidator.tests

            data class Test(val p0: Int)
        """.trimIndent().asSourceFile()

        val generator = ValidatorFileGenerator("xyz.uthofficial.ezvalidator.ksp")
        val result = generator.testWith(
            listOf(
                com.squareup.kotlinpoet.FunSpec.builder("validateTest")
                    .addParameters(listOf(ParameterSpec.builder("p0", String::class).build()))
                    .build()
            ),
            source
        )

        result shouldNotBeNull {
            compilationResult.exitCode shouldBeEqual KotlinCompilation.ExitCode.OK
            result.generatedFileSpec shouldNotBeNull {
                packageName shouldBeEqual "xyz.uthofficial.ezvalidator.ksp"
                name shouldBeEqual "TestValidator"
                funSpecs.apply {
                    shouldExist {
                        it.name == "validateTest"
                    }
                    toString() shouldContain "val res_validateTest = validateTest(p0)"
                }
            }
        }
    }
})