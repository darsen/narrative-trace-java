package ai.narrativetrace.diagrams;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import org.junit.jupiter.api.Test;

class MermaidSequenceDiagramRendererTest {

  private final MermaidSequenceDiagramRenderer renderer = new MermaidSequenceDiagramRenderer();

  @Test
  void rendersSingleCallAsParticipantMessageAndReply() {
    var node =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "calculateTotal",
                List.of(new ParameterCapture("orderId", "\"O-123\"", false))),
            List.of(),
            new TraceOutcome.Returned("99.0"),
            10_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var diagram = renderer.render(tree);

    assertThat(diagram).startsWith("sequenceDiagram");
    assertThat(diagram).contains("participant OrderService");
    assertThat(diagram).contains("OrderService->>OrderService: calculateTotal(orderId)");
    assertThat(diagram).contains("OrderService-->>OrderService: 99.0");
  }

  @Test
  void rendersNestedCallsWithCorrectParticipantOrderingAndFlow() {
    var inventoryCall =
        new TraceNode(
            new MethodSignature(
                "InventoryService",
                "reserveStock",
                List.of(new ParameterCapture("sku", "\"SKU-1\"", false))),
            List.of(),
            new TraceOutcome.Returned("true"),
            5_000_000L);
    var paymentCall =
        new TraceNode(
            new MethodSignature(
                "PaymentService", "charge", List.of(new ParameterCapture("amount", "99.0", false))),
            List.of(),
            new TraceOutcome.Returned("\"TX-456\""),
            8_000_000L);
    var root =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("orderId", "\"O-123\"", false))),
            List.of(inventoryCall, paymentCall),
            new TraceOutcome.Returned("\"OK\""),
            20_000_000L);
    var tree = new DefaultTraceTree(List.of(root));

    var diagram = renderer.render(tree);

    // Participants appear in order of first encounter
    assertThat(diagram).contains("participant OrderService");
    assertThat(diagram).contains("participant InventoryService");
    assertThat(diagram).contains("participant PaymentService");
    // Nested calls go from parent to child
    assertThat(diagram).contains("OrderService->>OrderService: placeOrder(orderId)");
    assertThat(diagram).contains("OrderService->>InventoryService: reserveStock(sku)");
    assertThat(diagram).contains("InventoryService-->>OrderService: true");
    assertThat(diagram).contains("OrderService->>PaymentService: charge(amount)");
    assertThat(diagram).contains("PaymentService-->>OrderService: \"TX-456\"");
    assertThat(diagram).contains("OrderService-->>OrderService: \"OK\"");
  }

  @Test
  void rendersExceptionPathsWithCrossNotation() {
    var failingCall =
        new TraceNode(
            new MethodSignature(
                "PaymentService", "charge", List.of(new ParameterCapture("amount", "99.0", false))),
            List.of(),
            new TraceOutcome.Threw(new RuntimeException("Insufficient funds")),
            3_000_000L);
    var root =
        new TraceNode(
            new MethodSignature("OrderService", "placeOrder", List.of()),
            List.of(failingCall),
            new TraceOutcome.Threw(new RuntimeException("Insufficient funds")),
            5_000_000L);
    var tree = new DefaultTraceTree(List.of(root));

    var diagram = renderer.render(tree);

    assertThat(diagram).contains("PaymentService-xOrderService: RuntimeException");
    assertThat(diagram).contains("OrderService-xOrderService: RuntimeException");
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

    assertThat(diagram).contains("OrderService-->>OrderService: null");
  }

  @Test
  void renderWithAliasesHandlesSingleUpperCaseCharClassName() {
    var node =
        new TraceNode(
            new MethodSignature("A", "run", List.of()),
            List.of(),
            new TraceOutcome.Returned("true"),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var diagram = renderer.renderWithAliases(tree);

    assertThat(diagram).contains("participant A as A");
  }

  @Test
  void rendersWithAliasesUsingTwoLetterAbbreviations() {
    var child =
        new TraceNode(
            new MethodSignature("InventoryService", "checkStock", List.of()),
            List.of(),
            new TraceOutcome.Returned("true"),
            1_000_000L);
    var root =
        new TraceNode(
            new MethodSignature("OrderService", "placeOrder", List.of()),
            List.of(child),
            new TraceOutcome.Returned("\"OK\""),
            5_000_000L);
    var tree = new DefaultTraceTree(List.of(root));

    var diagram = renderer.renderWithAliases(tree);

    assertThat(diagram).contains("participant OS as OrderService");
    assertThat(diagram).contains("participant IS as InventoryService");
    assertThat(diagram).contains("OS->>OS: placeOrder()");
    assertThat(diagram).contains("OS->>IS: checkStock()");
    assertThat(diagram).contains("IS-->>OS: true");
  }

  @Test
  void renderWithAliasesHandlesClassNameWithNoUppercaseLetters() {
    var node =
        new TraceNode(
            new MethodSignature("scheduler", "run", List.of()),
            List.of(),
            new TraceOutcome.Returned("true"),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var diagram = renderer.renderWithAliases(tree);

    // Should not produce "participant  as scheduler" (empty alias)
    assertThat(diagram).doesNotContain("participant  as");
    assertThat(diagram).contains("participant scheduler as scheduler");
  }

  @Test
  void renderWithAliasesDeduplicatesCollidingAliases() {
    var child =
        new TraceNode(
            new MethodSignature("OutdoorService", "check", List.of()),
            List.of(),
            new TraceOutcome.Returned("true"),
            1_000_000L);
    var root =
        new TraceNode(
            new MethodSignature("OrderService", "process", List.of()),
            List.of(child),
            new TraceOutcome.Returned("\"OK\""),
            5_000_000L);
    var tree = new DefaultTraceTree(List.of(root));

    var diagram = renderer.renderWithAliases(tree);

    // Both would produce "OS" — they must get distinct aliases
    var lines = diagram.lines().filter(l -> l.trim().startsWith("participant")).toList();
    var aliases = lines.stream().map(l -> l.trim().split(" ")[1]).toList();
    assertThat(aliases).doesNotHaveDuplicates();
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
        .contains("\"com.example.OrderService\"->>\"com.example.OrderService\": placeOrder()");
  }

  @Test
  void renderWithAliasesRendersExceptionAndMultipleParams() {
    var child =
        new TraceNode(
            new MethodSignature(
                "PaymentService",
                "charge",
                List.of(
                    new ParameterCapture("amount", "99.0", false),
                    new ParameterCapture("currency", "\"USD\"", false))),
            List.of(),
            new TraceOutcome.Threw(new RuntimeException("fail")),
            1_000_000L);
    var root =
        new TraceNode(
            new MethodSignature("OrderService", "placeOrder", List.of()),
            List.of(child),
            new TraceOutcome.Threw(new RuntimeException("fail")),
            5_000_000L);
    var tree = new DefaultTraceTree(List.of(root));

    var diagram = renderer.renderWithAliases(tree);

    assertThat(diagram).contains("OS->>PS: charge(amount, currency)");
    assertThat(diagram).contains("PS-xOS: RuntimeException");
    assertThat(diagram).contains("OS-xOS: RuntimeException");
  }

  @Test
  void aliasCollisionWithMultipleSuffixes() {
    // Three classes whose uppercase abbreviation is "OS": OrderService, OtherService, OnlineService
    var grandchild =
        new TraceNode(
            new MethodSignature("OnlineService", "check", List.of()),
            List.of(),
            new TraceOutcome.Returned("true"));
    var child =
        new TraceNode(
            new MethodSignature("OtherService", "validate", List.of()),
            List.of(grandchild),
            new TraceOutcome.Returned("true"));
    var root =
        new TraceNode(
            new MethodSignature("OrderService", "process", List.of()),
            List.of(child),
            new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(root));

    var diagram = renderer.renderWithAliases(tree);

    // Should have three distinct aliases: OS, OS2, OS3
    assertThat(diagram).contains("participant OS as OrderService");
    assertThat(diagram).contains("participant OS2 as OtherService");
    assertThat(diagram).contains("participant OS3 as OnlineService");
  }
}
