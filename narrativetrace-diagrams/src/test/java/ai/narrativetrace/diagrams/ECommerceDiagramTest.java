package ai.narrativetrace.diagrams;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ECommerceDiagramTest {

    @Test
    void ecommerceExampleProducesValidMermaidSequenceDiagram() {
        var customerLookup = new TraceNode(
                new MethodSignature("CustomerService", "findCustomer", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"Customer{name=Alice}\""),
                2_000_000L
        );
        var inventoryReserve = new TraceNode(
                new MethodSignature("InventoryService", "reserveStock", List.of(
                        new ParameterCapture("productId", "\"P-456\"", false),
                        new ParameterCapture("quantity", "2", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"RES-789\""),
                3_000_000L
        );
        var paymentCharge = new TraceNode(
                new MethodSignature("PaymentService", "chargeCustomer", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false),
                        new ParameterCapture("amount", "59.98", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"TX-101\""),
                5_000_000L
        );
        var root = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false),
                        new ParameterCapture("productId", "\"P-456\"", false),
                        new ParameterCapture("quantity", "2", false)
                )),
                List.of(customerLookup, inventoryReserve, paymentCharge),
                new TraceOutcome.Returned("\"OrderResult{orderId=ORD-001}\""),
                15_000_000L
        );
        var tree = new DefaultTraceTree(List.of(root));

        var mermaid = new MermaidSequenceDiagramRenderer().render(tree);

        assertThat(mermaid).startsWith("sequenceDiagram");
        // All participants present
        assertThat(mermaid).contains("participant OrderService");
        assertThat(mermaid).contains("participant CustomerService");
        assertThat(mermaid).contains("participant InventoryService");
        assertThat(mermaid).contains("participant PaymentService");
        // Full flow
        assertThat(mermaid).contains("OrderService->>OrderService: placeOrder(customerId, productId, quantity)");
        assertThat(mermaid).contains("OrderService->>CustomerService: findCustomer(customerId)");
        assertThat(mermaid).contains("OrderService->>InventoryService: reserveStock(productId, quantity)");
        assertThat(mermaid).contains("OrderService->>PaymentService: chargeCustomer(customerId, amount)");
    }
}
