package ai.narrativetrace.core.event;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class TraceNodeTest {

    @Test
    void representsLeafCall() {
        var signature = new MethodSignature("OrderService", "placeOrder", List.of());
        var outcome = new TraceOutcome.Returned("\"order-42\"");
        var node = new TraceNode(signature, List.of(), outcome);

        assertThat(node.signature()).isSameAs(signature);
        assertThat(node.children()).isEmpty();
        assertThat(node.outcome()).isSameAs(outcome);
    }

    @Test
    void representsNestedTree() {
        var childSig = new MethodSignature("InventoryService", "checkStock", List.of());
        var child = new TraceNode(childSig, List.of(), new TraceOutcome.Returned("true"));

        var parentSig = new MethodSignature("OrderService", "placeOrder", List.of());
        var parent = new TraceNode(parentSig, List.of(child), new TraceOutcome.Returned("\"order-42\""));

        assertThat(parent.children()).hasSize(1);
        assertThat(parent.children().get(0).signature().className()).isEqualTo("InventoryService");
    }

    @Test
    void capturesDurationInNanoseconds() {
        var signature = new MethodSignature("OrderService", "placeOrder", List.of());
        var outcome = new TraceOutcome.Returned("\"order-42\"");
        long durationNanos = 24_000_000L; // 24ms

        var node = new TraceNode(signature, List.of(), outcome, durationNanos);

        assertThat(node.durationNanos()).isEqualTo(24_000_000L);
    }
}
