package ai.narrativetrace.core.render;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    @Test
    void rendersLeafCallWithBoldMethodInlineCodeValuesAndDuration() {
        var node = new TraceNode(
                new MethodSignature("OrderValidator", "validate", List.of(
                        new ParameterCapture("cartId", "\"CART-77\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"valid\""),
                2_000_000L // 2ms
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("- **OrderValidator.validate**(cartId: `\"CART-77\"`) → `\"valid\"` — 2ms");
    }

    @Test
    void rendersNestedCallsAsMarkdownList() {
        var child = new TraceNode(
                new MethodSignature("InventoryService", "reserve", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("true"),
                24_000_000L
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(child),
                new TraceOutcome.Returned("\"order-42\""),
                412_000_000L
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("- **OrderService.placeOrder**(customerId: `\"C-123\"`) — 412ms");
        assertThat(result).contains("  - **InventoryService.reserve**(customerId: `\"C-123\"`) → `true` — 24ms");
        assertThat(result).contains("  - → `\"order-42\"`");
    }

    @Test
    void rendersExceptionInBlockquote() {
        var node = new TraceNode(
                new MethodSignature("PaymentGateway", "charge", List.of(
                        new ParameterCapture("amount", "97.14", false)
                )),
                List.of(),
                new TraceOutcome.Threw(new IllegalStateException("Card expired")),
                203_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("❌");
        assertThat(result).contains("`IllegalStateException`");
        assertThat(result).contains("Card expired");
    }

    @Test
    void rendersRedactedParamsInInlineCode() {
        var node = new TraceNode(
                new MethodSignature("AuthService", "login", List.of(
                        new ParameterCapture("username", "\"admin\"", false),
                        new ParameterCapture("password", "[REDACTED]", true)
                )),
                List.of(),
                new TraceOutcome.Returned("true"),
                5_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("username: `\"admin\"`");
        assertThat(result).contains("password: `[REDACTED]`");
    }

    @Test
    void rendersSlowThresholdMarker() {
        var node = new TraceNode(
                new MethodSignature("PaymentGateway", "charge", List.of(
                        new ParameterCapture("amount", "242.95", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"ok\""),
                340_000_000L // 340ms > 200ms threshold
        );
        var tree = new DefaultTraceTree(List.of(node));

        var renderer = new MarkdownRenderer(200); // 200ms threshold
        var result = renderer.render(tree);

        assertThat(result).contains("340ms");
        assertThat(result).contains("⚠️ slow");
    }

    @Test
    void doesNotMarkFastCallsAsSlow() {
        var node = new TraceNode(
                new MethodSignature("OrderValidator", "validate", List.of()),
                List.of(),
                new TraceOutcome.Returned("\"valid\""),
                2_000_000L // 2ms
        );
        var tree = new DefaultTraceTree(List.of(node));

        var renderer = new MarkdownRenderer(200);
        var result = renderer.render(tree);

        assertThat(result).doesNotContain("⚠️");
    }

    @Test
    void rendersFullDocumentWithFrontmatterAndScenarioHeader() {
        var child = new TraceNode(
                new MethodSignature("InventoryService", "reserve", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("true"),
                24_000_000L
        );
        var root = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(child),
                new TraceOutcome.Returned("\"order-42\""),
                412_000_000L
        );
        var tree = new DefaultTraceTree(List.of(root));
        var metadata = new TraceMetadata("Customer places order", "pass");

        var result = new MarkdownRenderer().renderDocument(tree, metadata);

        assertThat(result).startsWith("---\n");
        assertThat(result).contains("type: trace");
        assertThat(result).contains("scenario: Customer places order");
        assertThat(result).contains("---\n\n## Trace: OrderService.placeOrder");
        assertThat(result).contains("**Scenario:** Customer places order");
        assertThat(result).contains("**Duration:** 412ms | **Result:** pass");
        assertThat(result).contains("### Call Flow");
        assertThat(result).contains("- **OrderService.placeOrder**");
    }

    @Test
    void rendersNarrationAsItalicsBelowMethodEntry() {
        var child = new TraceNode(
                new MethodSignature("InventoryService", "reserve", List.of()),
                List.of(),
                new TraceOutcome.Returned("true"),
                24_000_000L
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder",
                        List.of(new ParameterCapture("customerId", "\"C-123\"", false),
                                new ParameterCapture("quantity", "5", false)),
                        "Placing order of 5 units for customer C-123", null),
                List.of(child),
                new TraceOutcome.Returned("\"order-42\""),
                412_000_000L
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("*Placing order of 5 units for customer C-123*");
    }

    @Test
    void renderDocumentWithEmptyTreeSkipsTraceHeader() {
        var tree = new DefaultTraceTree(List.of());
        var metadata = new TraceMetadata("Empty scenario", "pass");

        var result = new MarkdownRenderer().renderDocument(tree, metadata);

        assertThat(result).contains("---");
        assertThat(result).doesNotContain("## Trace:");
        assertThat(result).doesNotContain("### Call Flow");
    }

    @Test
    void rendersParentNodeWithThrewOutcome() {
        var child = new TraceNode(
                new MethodSignature("PaymentService", "charge", List.of()),
                List.of(),
                new TraceOutcome.Threw(new RuntimeException("declined")),
                5_000_000L
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of()),
                List.of(child),
                new TraceOutcome.Threw(new RuntimeException("order failed")),
                10_000_000L
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("❌ `RuntimeException`: order failed");
    }

    @Test
    void rendersErrorContextForLeafNodeWithThrewOutcome() {
        var node = new TraceNode(
                new MethodSignature("PaymentService", "charge",
                        List.of(new ParameterCapture("amount", "99.95", false)),
                        null, "Payment failed for amount 99.95"),
                List.of(),
                new TraceOutcome.Threw(new IllegalStateException("Card expired")),
                5_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("Payment failed for amount 99.95");
    }

    @Test
    void rendersNestedLeafExceptionWithCorrectIndentation() {
        var child = new TraceNode(
                new MethodSignature("PaymentService", "charge", List.of(),
                        null, "Payment failed"),
                List.of(),
                new TraceOutcome.Threw(new IllegalStateException("Card expired")),
                5_000_000L
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of()),
                List.of(child),
                new TraceOutcome.Threw(new RuntimeException("order failed")),
                10_000_000L
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new MarkdownRenderer().render(tree);

        // Child is at depth 1, so inline exception should be indented with 4 spaces (depth+1 = 2 repeats of "  ")
        assertThat(result).contains("\n    > ");
    }

    @Test
    void rendersErrorContextInClosingOutcome() {
        var child = new TraceNode(
                new MethodSignature("PaymentService", "charge", List.of()),
                List.of(),
                new TraceOutcome.Threw(new RuntimeException("declined"))
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of(),
                        null, "Order processing failed"),
                List.of(child),
                new TraceOutcome.Threw(new RuntimeException("order failed"))
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).contains("Order processing failed");
    }

    @Test
    void rendersLeafNodeWithZeroDurationWithoutDurationMarker() {
        var node = new TraceNode(
                new MethodSignature("Svc", "method", List.of()),
                List.of(),
                new TraceOutcome.Returned("\"ok\""),
                0L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new MarkdownRenderer().render(tree);

        assertThat(result).doesNotContain("ms");
    }
}
