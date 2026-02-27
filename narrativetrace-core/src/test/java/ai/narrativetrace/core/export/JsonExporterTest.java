package ai.narrativetrace.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.render.TraceMetadata;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonExporterTest {

  @Test
  void exportsSingleLeafNodeAsEnterAndExitEvents() {
    var node =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false))),
            List.of(),
            new TraceOutcome.Returned("\"order-42\""),
            412_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var json = new JsonExporter().export(tree);

    assertThat(json).contains("\"type\": \"enter\"");
    assertThat(json).contains("\"type\": \"exit\"");
    assertThat(json).contains("\"class\": \"OrderService\"");
    assertThat(json).contains("\"method\": \"placeOrder\"");
    assertThat(json).contains("\"customerId\": \"\\\"C-123\\\"\"");
    assertThat(json).contains("\"durationMs\": 412");
  }

  @Test
  void exportsNestedNodesWithDepthAndParentId() {
    var child =
        new TraceNode(
            new MethodSignature("InventoryService", "reserve", List.of()),
            List.of(),
            new TraceOutcome.Returned("true"),
            24_000_000L);
    var root =
        new TraceNode(
            new MethodSignature("OrderService", "placeOrder", List.of()),
            List.of(child),
            new TraceOutcome.Returned("\"order-42\""),
            412_000_000L);
    var tree = new DefaultTraceTree(List.of(root));

    var json = new JsonExporter().export(tree);

    // Root enter at depth 0, parentId null
    assertThat(json).contains("\"class\": \"OrderService\"");
    assertThat(json).contains("\"depth\": 0");
    assertThat(json).contains("\"parentId\": null");

    // Child enter at depth 1, parentId 1
    assertThat(json).contains("\"class\": \"InventoryService\"");
    assertThat(json).contains("\"depth\": 1");
    assertThat(json).contains("\"parentId\": 1");
  }

  @Test
  void exportsExceptionAsErrorEventType() {
    var node =
        new TraceNode(
            new MethodSignature("PaymentGateway", "charge", List.of()),
            List.of(),
            new TraceOutcome.Threw(new IllegalStateException("Card expired")),
            203_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var json = new JsonExporter().export(tree);

    assertThat(json).contains("\"type\": \"error\"");
    assertThat(json).contains("\"type\": \"IllegalStateException\"");
    assertThat(json).contains("\"message\": \"Card expired\"");
    assertThat(json).contains("\"durationMs\": 203");
  }

  @Test
  void serializesParametersAccordingToSpecRules() {
    var node =
        new TraceNode(
            new MethodSignature(
                "AuthService",
                "login",
                List.of(
                    new ParameterCapture("username", "\"admin\"", false),
                    new ParameterCapture("count", "42", false),
                    new ParameterCapture("active", "true", false),
                    new ParameterCapture("password", "[REDACTED]", true),
                    new ParameterCapture("nullParam", "null", false))),
            List.of(),
            new TraceOutcome.Returned("true"),
            5_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var json = new JsonExporter().export(tree);

    assertThat(json).contains("\"username\": \"\\\"admin\\\"\"");
    assertThat(json).contains("\"count\": \"42\"");
    assertThat(json).contains("\"active\": \"true\"");
    assertThat(json).contains("\"password\": \"[REDACTED]\"");
    assertThat(json).contains("\"nullParam\": \"null\"");
  }

  @Test
  void exportsFullTraceDocumentWithVersionAndScenarioMetadata() {
    var node =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false))),
            List.of(),
            new TraceOutcome.Returned("\"order-42\""),
            412_000_000L);
    var tree = new DefaultTraceTree(List.of(node));
    var metadata = new TraceMetadata("Customer places order", "pass");

    var json = new JsonExporter().exportDocument(tree, metadata);

    assertThat(json).contains("\"version\": \"1.0\"");
    assertThat(json).contains("\"scenario\":");
    assertThat(json).contains("\"name\": \"Customer places order\"");
    assertThat(json).contains("\"result\": \"pass\"");
    assertThat(json).contains("\"durationMs\": 412");
    assertThat(json).contains("\"events\":");
    assertThat(json).contains("\"type\": \"enter\"");
    assertThat(json).contains("\"type\": \"exit\"");
  }

  @Test
  void exportDocumentWithEmptyTreeProducesValidJson() {
    var tree = new DefaultTraceTree(List.of());
    var metadata = new TraceMetadata("Empty", "pass");

    var json = new JsonExporter().exportDocument(tree, metadata);

    assertThat(json).doesNotContain(",\n  }");
  }

  @Test
  void exportDocumentWithEmptyTreeOmitsDuration() {
    var tree = new DefaultTraceTree(List.of());
    var metadata = new TraceMetadata("Empty", "pass");

    var json = new JsonExporter().exportDocument(tree, metadata);

    assertThat(json).contains("\"version\": \"1.0\"");
    assertThat(json).contains("\"name\": \"Empty\"");
    assertThat(json).doesNotContain("\"durationMs\"");
  }

  @Test
  void serializesCustomObjectParamUsingToString() {
    var node =
        new TraceNode(
            new MethodSignature(
                "Svc", "method", List.of(new ParameterCapture("obj", "[1, 2]", false))),
            List.of(),
            new TraceOutcome.Returned("\"ok\""),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var json = new JsonExporter().export(tree);

    assertThat(json).contains("\"obj\": \"[1, 2]\"");
  }

  @Test
  void exportsExceptionWithNullMessage() {
    var node =
        new TraceNode(
            new MethodSignature("Svc", "method", List.of()),
            List.of(),
            new TraceOutcome.Threw(new NullPointerException()),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var json = new JsonExporter().export(tree);

    assertThat(json).contains("\"type\": \"error\"");
    assertThat(json).contains("\"message\": \"null\"");
  }

  @Test
  void emptyRenderedValueSerializesAsJsonNull() {
    var node =
        new TraceNode(
            new MethodSignature(
                "Svc",
                "method",
                List.of(
                    new ParameterCapture("empty", "", false),
                    new ParameterCapture("nul", null, false))),
            List.of(),
            new TraceOutcome.Returned("\"ok\""),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var json = new JsonExporter().export(tree);

    assertThat(json).contains("\"empty\": null");
    assertThat(json).contains("\"nul\": null");
  }

  @Test
  void multiRootTreeProducesCommaSeparatedEnterEvents() {
    var root1 =
        new TraceNode(
            new MethodSignature("A", "first", List.of()),
            List.of(),
            new TraceOutcome.Returned("1"),
            1_000_000L);
    var root2 =
        new TraceNode(
            new MethodSignature("B", "second", List.of()),
            List.of(),
            new TraceOutcome.Returned("2"),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(root1, root2));

    var json = new JsonExporter().export(tree);

    // Both roots present
    assertThat(json).contains("\"class\": \"A\"");
    assertThat(json).contains("\"class\": \"B\"");
    // JSON must parse — no missing commas between events
    // The second enter event must be preceded by a comma
    int secondEnterPos = json.indexOf("\"method\": \"second\"");
    assertThat(secondEnterPos).isGreaterThan(0);
    String beforeSecondEnter = json.substring(0, secondEnterPos);
    // Find the last closing brace before the second enter
    int lastBrace = beforeSecondEnter.lastIndexOf('}');
    String between = json.substring(lastBrace + 1, secondEnterPos);
    assertThat(between).contains(",");
  }

  @Test
  void bothEnterAndExitEventsContainDepthAndParentId() {
    var node =
        new TraceNode(
            new MethodSignature("Svc", "method", List.of()),
            List.of(),
            new TraceOutcome.Returned("\"ok\""),
            1_000_000L);
    var tree = new DefaultTraceTree(List.of(node));

    var json = new JsonExporter().export(tree);

    // A single node produces exactly 2 events (enter + exit)
    // Both must have depth and parentId
    long depthCount = countOccurrences(json, "\"depth\": 0");
    long parentIdCount = countOccurrences(json, "\"parentId\": null");
    assertThat(depthCount).isEqualTo(2);
    assertThat(parentIdCount).isEqualTo(2);
  }

  @Test
  void exportDocumentIncludesDurationInScenarioSection() {
    var node =
        new TraceNode(
            new MethodSignature("Svc", "method", List.of()),
            List.of(),
            new TraceOutcome.Returned("\"ok\""),
            500_000_000L);
    var tree = new DefaultTraceTree(List.of(node));
    var metadata = new TraceMetadata("Test scenario", "pass");

    var json = new JsonExporter().exportDocument(tree, metadata);

    // The durationMs must appear in the scenario section (before "events")
    int eventsPos = json.indexOf("\"events\"");
    String scenarioSection = json.substring(0, eventsPos);
    assertThat(scenarioSection).contains("\"durationMs\": 500");
  }

  private static long countOccurrences(String text, String substring) {
    long count = 0;
    int idx = text.indexOf(substring, 0);
    while (idx != -1) {
      count++;
      idx = text.indexOf(substring, idx + substring.length());
    }
    return count;
  }
}
