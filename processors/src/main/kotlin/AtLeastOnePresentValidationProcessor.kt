import arrow.core.Option
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class AtLeastOnePresentValidationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(AtLeastOnePresent::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        symbols.forEach { this.generateFactoryFunction(it) }
        TODO("Not yet implemented")
    }

    private fun generateFactoryFunction(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()

        val constructor = classDeclaration.primaryConstructor

        when {
            constructor == null -> {
                logger.error("Class $className must have a primary constructor to validate with", classDeclaration)
                return
            }
        }

        val parameterSpecs = constructor.parameters.toPoetParameterSpecs()
        val optionParamNames = constructor.parameters.findArrowOptionParamNames()

        val conditionCode = when {
            optionParamNames.isNotEmpty() -> optionParamNames.joinToString("||") { "${it}.isSome()" }
            else -> "true"
        }

        val constructorCallArgs = parameterSpecs.joinToString(", ") { it.name }

        val factoryFunction = FunSpec.builder("create$className")
            // Since it is a factory function where failure is expected, we return EITHER a validation error or the
            // successfully created one.
            .returns(classDeclaration.toClassName())
            // Add the parameters extracted from the original one.
            .addParameters(parameterSpecs)
            .addStatement("val isAnyPresent = $conditionCode")
    }

    private fun List<KSValueParameter>.toPoetParameterSpecs(): List<ParameterSpec> = this.map {
        val name = it.name!!.asString()
        val type = it.type.resolve().toTypeName()
        ParameterSpec.builder(name, type).build()
    }

    private fun List<KSValueParameter>.findArrowOptionParamNames(): List<String> = this.filter {
        it.type.resolve().declaration.qualifiedName?.asString() == Option::class.qualifiedName!!
    }
        .map { it.name!!.asString() }
}