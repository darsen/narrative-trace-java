package ai.narrativetrace.diagrams;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlantUmlSequenceDiagramRendererTest {

  private final PlantUmlSequenceDiagramRenderer renderer = new PlantUmlSequenceDiagramRenderer();

  @Test
  void rendersEquivalentPlantUmlSyntax() {
    var child =
        new TraceNode(
            new MethodSignature(
                "InventoryService",
                "checkStock",
                List.of(new ParameterCapture("sku", "\"SKU-1\"", false))),
            List.of(),
            new TraceOutcome.Returned("true"),
            5_000_000L);
    var root =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("orderId", "\"O-123\"", false))),
            List.of(child),
            new TraceOutcome.Returned("\"OK\""),
            20_000_000L);
    var tree = new DefaultTraceTree(List.of(root));

    var diagram = renderer.render(tree);

    assertThat(diagram).startsWith("@startuml");
    assertThat(diagram).endsWith("@enduml");
    assertThat(diagram).contains("participant OrderService");
    assertThat(diagram).contains("participant InventoryService");
    assertThat(diagram).contains("OrderService -> OrderService: placeOrder(orderId)");
    assertThat(diagram).contains("OrderService -> InventoryService: checkStock(sku)");
    assertThat(diagram).contains("InventoryService --> OrderService: true");
    assertThat(diagram).contains("OrderService --> OrderService: \"OK\"");
  }

  @Test
  void rendersExceptionWithRedArrow() {
    var node =
        new TraceNode(
            new MethodSignature("PaymentService", "charge", List.of()),
            List.of(),
            new TraceOutcome.Threw(new RuntimeException("fail")),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var diagram = renderer.render(tree);

    assertThat(diagram).contains("PaymentService -[#red]-> PaymentService: RuntimeException");
  }

  @Test
  void rendersNullReturnValueAsNull() {
    var node =
        new TraceNode(
            new MethodSignature("OrderService", "findOrder", List.of()),
            List.of(),
            new TraceOutcome.Returned("null"),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var diagram = renderer.render(tree);

    assertThat(diagram).contains("OrderService --> OrderService: null");
  }

  @Test
  void rendersParticipantWithDotsInQuotes() {
    var node =
        new TraceNode(
            new MethodSignature("com.example.OrderService", "placeOrder", List.of()),
            List.of(),
            new TraceOutcome.Returned("\"OK\""),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var diagram = renderer.render(tree);

    assertThat(diagram).contains("participant \"com.example.OrderService\"");
    assertThat(diagram)
        .contains("\"com.example.OrderService\" -> \"com.example.OrderService\": placeOrder()");
  }

  @Test
  void rendersMultipleParametersJoinedWithComma() {
    var node =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(
                    new ParameterCapture("orderId", "\"O-1\"", false),
                    new ParameterCapture("customerId", "\"C-1\"", false))),
            List.of(),
            new TraceOutcome.Returned("\"OK\""),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var diagram = renderer.render(tree);

    assertThat(diagram).contains("OrderService -> OrderService: placeOrder(orderId, customerId)");
  }
}
