package handlers

import Ok
import arrow.core.Either
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.tschuchort.compiletesting.*
import createCompilation
import createHandlerSpecificProcessorProvider
import createTestSourceFile
import errors.TooLessPresentedError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@Suppress("RedundantInterpolationPrefix")
@OptIn(ExperimentalCompilerApi::class)
class AtLeastOnePresentHandlerTest : FunSpec({
    lateinit var compilationResult: JvmCompilationResult
    var processorResult: Result<com.squareup.kotlinpoet.FunSpec>? = null
    lateinit var generatedFunSpec: com.squareup.kotlinpoet.FunSpec

    beforeSpec {
        val sourceFile = SourceFile.kotlin(
            "Test.kt", $$"""
                package xyz.uthofficial.tests
                import arrow.core.Option
                
                data class Test(val opt1: Option<String>, val opt2: Option<Int>, val opt3: Option<Double>)
        """, true
        )

        compilationResult = KotlinCompilation().apply {
            useKsp2()
            symbolProcessorProviders = mutableListOf(SymbolProcessorProvider {
                object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        processorResult = AtLeastOnePresentHandler().process(
                            resolver.getClassDeclarationByName(
                                resolver.getKSNameFromString("xyz.uthofficial.tests.Test")
                            )!!
                        )
                        return emptyList()
                    }
                }
            })
            sources = listOf(sourceFile)
            inheritClassPath = true
        }.compile()
    }

    test("should compile without errors") {
        createCompilation(
            listOf(
                createTestSourceFile(
                    "Test.kt",
                    "p0: Option<String>, p1: Option<Int>, p2: Option<Double>"
                )
            ), mutableListOf(createHandlerSpecificProcessorProvider(AtLeastOnePresentHandler()).first)
        ).compile().exitCode.shouldBeEqual(KotlinCompilation.ExitCode.OK)
    }

    test("should generate correct validation function signature") {
        processorResult shouldNotBeNull {
            generatedFunSpec = this.getOrThrow()
            generatedFunSpec.apply {
                name shouldBe "validateAtLeastOnePresent"
                parameters.size shouldBe 3
                returnType shouldBeEqual Either::class.asClassName()
                    .parameterizedBy(TooLessPresentedError::class.asClassName(), Ok::class.asClassName())
                modifiers shouldBeEqual KModifier.PRIVATE
            }
        }
    }

    test("should generate nothing when there are no Option parameters") {
        val sourceFile = SourceFile.kotlin(
            "Test.kt", $$"""
                package xyz.uthofficial.tests
                                
                data class TestNoOption(val str: String, val num: Int)
        """, true
        )

        val compilationResultNoOption = KotlinCompilation().apply {
            useKsp2()
            symbolProcessorProviders = mutableListOf(SymbolProcessorProvider {
                object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        val result = AtLeastOnePresentHandler().process(
                            resolver.getClassDeclarationByName(
                                resolver.getKSNameFromString("xyz.uthofficial.tests.TestNoOption")
                            )!!
                        )
                        result.isFailure shouldBe true
                        return emptyList()
                    }
                }
            })
            sources = listOf(sourceFile)
            inheritClassPath = true
        }.compile()
    }

    test("should ") {

    }
})