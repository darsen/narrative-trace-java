package ai.narrativetrace.examples.library

import ai.narrativetrace.core.context.ThreadLocalNarrativeContext
import ai.narrativetrace.core.render.IndentedTextRenderer
import ai.narrativetrace.core.render.ProseRenderer
import ai.narrativetrace.diagrams.MermaidSequenceDiagramRenderer
import ai.narrativetrace.proxy.NarrativeTraceProxy
import ai.narrativetrace.slf4j.Slf4jNarrativeContext
import org.slf4j.LoggerFactory

object LibraryExample {
    private val logger = LoggerFactory.getLogger(LibraryExample::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val context = Slf4jNarrativeContext(ThreadLocalNarrativeContext())
        val renderer = IndentedTextRenderer()
        val proseRenderer = ProseRenderer()
        val mermaidRenderer = MermaidSequenceDiagramRenderer()

        val catalog =
            NarrativeTraceProxy.trace(
                InMemoryCatalogService(),
                CatalogService::class.java,
                context,
            )
        val members =
            NarrativeTraceProxy.trace(
                InMemoryMemberService(),
                MemberService::class.java,
                context,
            )
        val lending =
            NarrativeTraceProxy.trace(
                DefaultLendingService(catalog, members),
                LendingService::class.java,
                context,
            )

        // --- Scenario 1: Successful borrow ---
        logger.info("=== Scenario 1: Successful Book Borrow ===\n")
        val receipt = lending.borrowBook("M-001", "978-0-13-468599-1")
        logger.info("Received: {}", receipt)
        val trace1 = context.captureTrace()
        logger.info("\n{}", renderer.render(trace1))
        logger.info("\n--- Prose ---\n")
        logger.info("\n{}", proseRenderer.render(trace1))
        logger.info("\n--- Mermaid ---\n")
        logger.info("\n{}", mermaidRenderer.render(trace1))

        // --- Scenario 2: Book unavailable ---
        context.reset()
        logger.info("\n=== Scenario 2: Book Unavailable ===\n")
        try {
            lending.borrowBook("M-001", "978-0-13-235088-4")
        } catch (e: BookUnavailableException) {
            // expected
        }
        val trace2 = context.captureTrace()
        logger.info("\n{}", renderer.render(trace2))
        logger.info("\n--- Prose ---\n")
        logger.info("\n{}", proseRenderer.render(trace2))
    }
}
