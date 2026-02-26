package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import ai.narrativetrace.core.render.ProseRenderer;
import ai.narrativetrace.diagrams.MermaidSequenceDiagramRenderer;
import ai.narrativetrace.proxy.NarrativeTraceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * E-commerce example using Spring {@code @EnableNarrativeTrace} auto-proxying
 * and {@code @Async} cross-thread propagation via the Micrometer bridge.
 *
 * <p>Correlation: in a Spring Boot app, Micrometer Tracing automatically puts
 * a {@code traceId} in MDC for every inbound HTTP request. Here we simulate
 * that with sequential IDs since there is no inbound request. The
 * {@link ContextPropagatingTaskDecorator} propagates both MDC and narrative
 * context to async threads, so log lines from both threads share the same
 * trace ID.</p>
 */
public class ECommerceExample {

    private static final Logger logger = LoggerFactory.getLogger(ECommerceExample.class);
    private static final AtomicInteger traceCounter = new AtomicInteger(1);

    private static void beginTrace(String label) {
        MDC.put("traceId", "trace-%03d".formatted(traceCounter.getAndIncrement()));
        logger.info("=== {} ===\n", label);
    }

    private static void endTrace() {
        MDC.remove("traceId");
    }

    public static void main(String[] args) {
        try (var spring = new AnnotationConfigApplicationContext(ECommerceConfig.class)) {
            var context = spring.getBean(NarrativeContext.class);
            var orders = spring.getBean(OrderService.class);
            var notifications = spring.getBean(NotificationService.class);
            var catalog = spring.getBean(ProductCatalogService.class);
            var executor = spring.getBean(ThreadPoolTaskExecutor.class);

            var renderer = new IndentedTextRenderer();
            var proseRenderer = new ProseRenderer();
            var mermaidRenderer = new MermaidSequenceDiagramRenderer();

            // --- Scenario 1: Happy path + @Async notification ---
            beginTrace("Scenario 1: Successful Order + Async Notification");
            var result = orders.placeOrder("C-1234", "SKU-MECHANICAL-KB", 2);
            notifications.notifyOrderPlaced("C-1234", result.orderId()).join();
            var trace1 = context.captureTrace();
            logger.info("\n{}", renderer.render(trace1));
            logger.info("\n--- Prose ---\n");
            logger.info("\n{}", proseRenderer.render(trace1));
            logger.info("\n--- Mermaid ---\n");
            logger.info("\n{}", mermaidRenderer.render(trace1));
            endTrace();

            // --- Scenario 2: Payment failure (inventory leak bug) ---
            context.reset();
            beginTrace("Scenario 2: Payment Failure — Inventory Leak Bug");
            try {
                orders.placeOrder("C-BROKE", "SKU-MOUSE-PAD", 3);
            } catch (PaymentDeclinedException e) {
                // expected
            }
            var trace2 = context.captureTrace();
            logger.info("\n{}", renderer.render(trace2));
            logger.info("\n  ^ Notice: InventoryService.reserve was called but InventoryService.release is missing from the trace.");
            logger.info("\n--- Prose ---\n");
            logger.info("\n{}", proseRenderer.render(trace2));
            logger.info("\n--- Mermaid ---\n");
            logger.info("\n{}", mermaidRenderer.render(trace2));
            endTrace();

            // --- Scenario 3: Flaky external notification service ---
            context.reset();
            beginTrace("Scenario 3: Flaky External Service");
            var flakyNotifications = NarrativeTraceProxy.trace(
                    new FlakyNotificationService(new JsonPlaceholderNotificationService(), 2),
                    NotificationService.class, context);
            flakyNotifications.notifyOrderPlaced("C-1234", "ORD-00001");
            try {
                flakyNotifications.notifyOrderPlaced("C-1234", "ORD-00002");
            } catch (ExternalServiceException e) {
                // expected
            }
            var trace3 = context.captureTrace();
            logger.info("\n{}", renderer.render(trace3));
            logger.info("\n--- Prose ---\n");
            logger.info("\n{}", proseRenderer.render(trace3));
            endTrace();

            // --- Scenario 4: Unknown customer ---
            context.reset();
            beginTrace("Scenario 4: Unknown Customer");
            try {
                orders.placeOrder("C-UNKNOWN", "SKU-MECHANICAL-KB", 1);
            } catch (IllegalArgumentException e) {
                // expected
            }
            var trace4 = context.captureTrace();
            logger.info("\n{}", renderer.render(trace4));
            logger.info("\n--- Prose ---\n");
            logger.info("\n{}", proseRenderer.render(trace4));
            endTrace();

            // --- Scenario 5: Out of stock ---
            context.reset();
            beginTrace("Scenario 5: Out of Stock");
            try {
                orders.placeOrder("C-1234", "SKU-USB-HUB", 9999);
            } catch (IllegalStateException e) {
                // expected
            }
            var trace5 = context.captureTrace();
            logger.info("\n{}", renderer.render(trace5));
            logger.info("\n--- Prose ---\n");
            logger.info("\n{}", proseRenderer.render(trace5));
            endTrace();

            // --- Scenario 6: Explicit async trace capture ---
            context.reset();
            beginTrace("Scenario 6: Explicit Async Trace Capture");

            catalog.lookupPrice("SKU-MECHANICAL-KB");
            var mainTrace = context.captureTrace();

            var asyncTrace = CompletableFuture.supplyAsync(() -> {
                catalog.lookupPrice("SKU-MOUSE-PAD");
                catalog.lookupPrice("SKU-USB-HUB");
                return context.captureTrace();
            }, executor).join();

            logger.info("Main thread trace:\n{}", renderer.render(mainTrace));
            logger.info("\nAsync thread trace:\n{}", renderer.render(asyncTrace));
            endTrace();

            executor.shutdown();
        }
    }
}
