package ai.narrativetrace.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateWarningCollectorTest {

  @Test
  void returnsEmptyForTraceWithNoNarrations() {
    var sig = new MethodSignature("OrderService", "placeOrder", List.of());
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var warnings = TemplateWarningCollector.collect(tree);

    assertThat(warnings).isEmpty();
  }

  @Test
  void returnsEmptyForFullyResolvedNarration() {
    var sig =
        new MethodSignature(
            "OrderService",
            "placeOrder",
            List.of(new ParameterCapture("id", "\"C-123\"", false)),
            "Placing order for C-123",
            null);
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var warnings = TemplateWarningCollector.collect(tree);

    assertThat(warnings).isEmpty();
  }

  @Test
  void detectsUnresolvedPlaceholderInNarration() {
    var sig =
        new MethodSignature(
            "OrderService", "placeOrder", List.of(), "Placing order for {custmerId}", null);
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var warnings = TemplateWarningCollector.collect(tree);

    assertThat(warnings).hasSize(1);
    assertThat(warnings.get(0).className()).isEqualTo("OrderService");
    assertThat(warnings.get(0).methodName()).isEqualTo("placeOrder");
    assertThat(warnings.get(0).placeholder()).isEqualTo("custmerId");
    assertThat(warnings.get(0).field()).isEqualTo("narration");
  }

  @Test
  void detectsUnresolvedPlaceholderInErrorContext() {
    var sig =
        new MethodSignature("OrderService", "placeOrder", List.of(), null, "Failed for {orderId}");
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var warnings = TemplateWarningCollector.collect(tree);

    assertThat(warnings).hasSize(1);
    assertThat(warnings.get(0).field()).isEqualTo("errorContext");
    assertThat(warnings.get(0).placeholder()).isEqualTo("orderId");
  }

  @Test
  void detectsUnresolvedInChildNodes() {
    var childSig =
        new MethodSignature("InventoryService", "reserve", List.of(), "Reserving {sku}", null);
    var child = new TraceNode(childSig, List.of(), new TraceOutcome.Returned("true"));
    var parentSig = new MethodSignature("OrderService", "placeOrder", List.of());
    var parent = new TraceNode(parentSig, List.of(child), new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(parent));

    var warnings = TemplateWarningCollector.collect(tree);

    assertThat(warnings).hasSize(1);
    assertThat(warnings.get(0).className()).isEqualTo("InventoryService");
    assertThat(warnings.get(0).placeholder()).isEqualTo("sku");
  }

  @Test
  void formatRendersReadableOutput() {
    var warnings =
        List.of(
            new TemplateWarningCollector.TemplateWarning(
                "OrderService", "placeOrder", "custmerId", "narration"));

    var output = TemplateWarningCollector.format(warnings);

    assertThat(output).contains("Unresolved template placeholder");
    assertThat(output).contains("OrderService.placeOrder");
    assertThat(output).contains("{custmerId}");
    assertThat(output).contains("narration");
  }

  @Test
  void formatReturnsEmptyStringForNoWarnings() {
    assertThat(TemplateWarningCollector.format(List.of())).isEmpty();
  }
}
