package ai.narrativetrace.core.render;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndentedTextRendererTest {

    @Test
    void rendersSingleLeafCall() {
        var node = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"order-42\"")
        );
        var tree = new DefaultTraceTree(List.of(node));

        var renderer = new IndentedTextRenderer();
        var result = renderer.render(tree);

        assertThat(result).isEqualTo("OrderService.placeOrder(customerId: \"C-123\") → \"order-42\"");
    }

    @Test
    void rendersNestedCallsWithIndentation() {
        var child = new TraceNode(
                new MethodSignature("InventoryService", "checkStock", List.of(
                        new ParameterCapture("itemId", "\"ITEM-1\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("true")
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(child),
                new TraceOutcome.Returned("\"order-42\"")
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).isEqualTo("""
                OrderService.placeOrder(customerId: "C-123")
                ├── InventoryService.checkStock(itemId: "ITEM-1") → true
                └── → "order-42\"""");
    }

    @Test
    void rendersRedactedParamsAsRedacted() {
        var node = new TraceNode(
                new MethodSignature("AuthService", "login", List.of(
                        new ParameterCapture("username", "\"admin\"", false),
                        new ParameterCapture("password", "[REDACTED]", true)
                )),
                List.of(),
                new TraceOutcome.Returned("true")
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).isEqualTo("AuthService.login(username: \"admin\", password: [REDACTED]) → true");
    }

    @Test
    void rendersDurationWhenPresent() {
        var node = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"order-42\""),
                24_000_000L // 24ms
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).isEqualTo(
                "OrderService.placeOrder(customerId: \"C-123\") → \"order-42\" — 24ms");
    }

    @Test
    void rendersNarrationBelowMethodEntry() {
        var child = new TraceNode(
                new MethodSignature("InventoryService", "reserve", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false),
                        new ParameterCapture("quantity", "5", false)
                )),
                List.of(),
                new TraceOutcome.Returned("true")
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder",
                        List.of(new ParameterCapture("customerId", "\"C-123\"", false),
                                new ParameterCapture("quantity", "5", false)),
                        "Placing order of 5 units for customer C-123", null),
                List.of(child),
                new TraceOutcome.Returned("\"order-42\"")
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).contains("OrderService.placeOrder(customerId: \"C-123\", quantity: 5)");
        assertThat(result).contains("│   // Placing order of 5 units for customer C-123");
        assertThat(result).contains("├── InventoryService.reserve");
    }

    @Test
    void rendersExceptionOutcome() {
        var node = new TraceNode(
                new MethodSignature("PaymentService", "charge", List.of(
                        new ParameterCapture("amount", "99.95", false)
                )),
                List.of(),
                new TraceOutcome.Threw(new IllegalStateException("insufficient funds"))
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).isEqualTo("PaymentService.charge(amount: 99.95) !! IllegalStateException: insufficient funds");
    }

    @Test
    void rendersErrorContextForLeafNodeWithThrewOutcome() {
        var node = new TraceNode(
                new MethodSignature("PaymentService", "charge",
                        List.of(new ParameterCapture("amount", "99.95", false)),
                        null, "Payment failed for amount 99.95"),
                List.of(),
                new TraceOutcome.Threw(new IllegalStateException("Card expired"))
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).contains("Payment failed for amount 99.95");
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

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).contains("!! RuntimeException: order failed | Order processing failed");
    }

    @Test
    void rendersParentNodeWithThrewOutcome() {
        var child = new TraceNode(
                new MethodSignature("PaymentService", "charge", List.of()),
                List.of(),
                new TraceOutcome.Threw(new RuntimeException("declined"))
        );
        var parent = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of()),
                List.of(child),
                new TraceOutcome.Threw(new RuntimeException("order failed"))
        );
        var tree = new DefaultTraceTree(List.of(parent));

        var result = new IndentedTextRenderer().render(tree);

        assertThat(result).contains("!! RuntimeException: order failed");
    }
}
