package xyz.uthofficial.ezvalidator.processors

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@AutoService(SymbolProcessorProvider::class)
class ValidationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val packageName = environment.options["ez-validator.package"] ?: "xyz.uthofficial.ezvalidator.ksp"
        val fileGenerator = ValidatorFileGenerator(packageName)
        return ValidationProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            fileGenerator = fileGenerator
        )
    }
}
