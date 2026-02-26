package ai.narrativetrace.core.render;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrontmatterBuilderTest {

    @Test
    void buildsYamlFrontmatterFromTraceTree() {
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

        var frontmatter = new FrontmatterBuilder()
                .scenario("Customer places order")
                .build(tree);

        assertThat(frontmatter).startsWith("---\n");
        assertThat(frontmatter).endsWith("---\n");
        assertThat(frontmatter).contains("type: trace");
        assertThat(frontmatter).contains("scenario: Customer places order");
        assertThat(frontmatter).contains("entry_point: OrderService.placeOrder");
        assertThat(frontmatter).contains("duration_ms: 412");
        assertThat(frontmatter).contains("method_count: 2");
        assertThat(frontmatter).contains("error_count: 0");
    }

    @Test
    void emptyTreeProducesZeroMethodAndErrorCounts() {
        var tree = new DefaultTraceTree(List.of());

        var frontmatter = new FrontmatterBuilder().build(tree);

        assertThat(frontmatter).contains("method_count: 0");
        assertThat(frontmatter).contains("error_count: 0");
        assertThat(frontmatter).doesNotContain("entry_point");
        assertThat(frontmatter).doesNotContain("scenario");
    }

    @Test
    void treeWithErrorsCountsAllErrors() {
        var failChild = new TraceNode(
                new MethodSignature("PaymentService", "charge", List.of()),
                List.of(),
                new TraceOutcome.Threw(new RuntimeException("declined")),
                5_000_000L
        );
        var root = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of()),
                List.of(failChild),
                new TraceOutcome.Threw(new RuntimeException("order failed")),
                10_000_000L
        );
        var tree = new DefaultTraceTree(List.of(root));

        var frontmatter = new FrontmatterBuilder()
                .scenario("Order fails")
                .build(tree);

        assertThat(frontmatter).contains("error_count: 2");
        assertThat(frontmatter).contains("method_count: 2");
    }
}
