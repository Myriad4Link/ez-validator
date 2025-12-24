import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import handlers.ValidationHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ValidationProcessorTest {

    @Test
    fun `should call handler when matching annotation is found`() {
        val logger = mockk<KSPLogger>(relaxed = true)
        val resolver = mockk<Resolver>()
        val classDeclaration = mockk<KSClassDeclaration>(relaxed = true)
        val annotation = mockk<KSAnnotation>(relaxed = true)
        val handler = mockk<ValidationHandler>(relaxed = true)

        every { resolver.getSymbolsWithAnnotation("AtLeastOnePresent") } returns sequenceOf(classDeclaration)
        every { classDeclaration.annotations } returns sequenceOf(annotation)
        every { handler.canProcess(annotation) } returns true
        every { handler.process(classDeclaration) } returns Result.success(FunSpec.builder("test").build())

        val processor = ValidationProcessor(
            codeGenerator = mockk(relaxed = true),
            logger = logger,
            options = emptyMap(),
            handlers = listOf(handler)
        )

        processor.process(resolver)

        verify { handler.process(classDeclaration) }
    }

    @Test
    fun `should log error when handler fails`() {
        val logger = mockk<KSPLogger>(relaxed = true)
        val resolver = mockk<Resolver>()
        val classDeclaration = mockk<KSClassDeclaration>(relaxed = true)
        val annotation = mockk<KSAnnotation>(relaxed = true)
        val handler = mockk<ValidationHandler>(relaxed = true)

        every { resolver.getSymbolsWithAnnotation("AtLeastOnePresent") } returns sequenceOf(classDeclaration)
        every { classDeclaration.annotations } returns sequenceOf(annotation)
        every { handler.canProcess(annotation) } returns true
        every { handler.process(classDeclaration) } returns Result.failure(RuntimeException("Test error"))

        val processor = ValidationProcessor(
            codeGenerator = mockk(relaxed = true),
            logger = logger,
            options = emptyMap(),
            handlers = listOf(handler)
        )

        processor.process(resolver)
        verify { logger.error("Test error", classDeclaration) }
    }
}
