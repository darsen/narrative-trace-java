package ai.narrativetrace.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import ai.narrativetrace.core.tree.TraceTree;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TraceTestSupportTest {

  @Test
  void extensionForMarkdownFormat() {
    assertThat(TraceTestSupport.extensionForFormat("markdown")).isEqualTo(".md");
  }

  @Test
  void extensionForTextFormat() {
    assertThat(TraceTestSupport.extensionForFormat("text")).isEqualTo(".txt");
  }

  @Test
  void extensionForMermaidFormat() {
    assertThat(TraceTestSupport.extensionForFormat("mermaid")).isEqualTo(".mmd");
  }

  @Test
  void extensionForPlantUmlFormat() {
    assertThat(TraceTestSupport.extensionForFormat("plantuml")).isEqualTo(".puml");
  }

  @Test
  void buildFailureReportFormatsScenarioAndTrace() {
    var report =
        TraceTestSupport.buildFailureReport(
            "Scenario: Customer places order", "Service.doWork() → ok");

    assertThat(report)
        .isEqualTo(
            "\n\nScenario: Customer places order\n\nExecution trace:\nService.doWork() → ok");
  }

  @Test
  void renderForTextFormat() {
    var trace = traceWithOneCall();

    var result =
        TraceTestSupport.renderForFormat("text", trace, "customerPlacesOrder()", false, null, null);

    assertThat(result).contains("Scenario: Customer places order");
    assertThat(result).contains("Service.doWork");
  }

  @Test
  void renderForMarkdownFormat() {
    var trace = traceWithOneCall();

    var result =
        TraceTestSupport.renderForFormat(
            "markdown", trace, "customerPlacesOrder()", false, null, null);

    assertThat(result).contains("Service.doWork");
    assertThat(result).contains("Customer places order");
  }

  @Test
  void renderForMermaidFormat() {
    var trace = traceWithOneCall();
    var mermaidRenderer = stubRenderer("sequenceDiagram\n  Test->>Service: doWork");

    var result =
        TraceTestSupport.renderForFormat("mermaid", trace, "test", false, mermaidRenderer, null);

    assertThat(result).startsWith("sequenceDiagram");
    assertThat(result).contains("Service");
  }

  @Test
  void renderForPlantUmlFormat() {
    var trace = traceWithOneCall();
    var plantumlRenderer = stubRenderer("@startuml\nTest -> Service: doWork\n@enduml");

    var result =
        TraceTestSupport.renderForFormat("plantuml", trace, "test", false, null, plantumlRenderer);

    assertThat(result).startsWith("@startuml");
    assertThat(result).contains("Service");
  }

  private static TraceTree traceWithOneCall() {
    var node =
        new TraceNode(
            new MethodSignature("Service", "doWork", List.of()),
            List.of(),
            new TraceOutcome.Returned("\"ok\""));
    return new DefaultTraceTree(List.of(node));
  }

  private static TraceTree emptyTrace() {
    return new DefaultTraceTree(List.of());
  }

  @Test
  void writeTraceFileWritesMarkdownAndPrintsPath(@TempDir Path tempDir) throws Exception {
    var trace = traceWithOneCall();
    var out = new ByteArrayOutputStream();
    var mermaid = stubRenderer("sequenceDiagram\n  Test->>Service: doWork");

    TraceTestSupport.writeTraceFile(
        "com.example.FooTest",
        "testSomething",
        "test something",
        trace,
        false,
        tempDir,
        new PrintStream(out),
        "markdown",
        mermaid,
        null);

    var file = tempDir.resolve("traces/FooTest/test_something.md");
    assertThat(file).exists();
    var content = Files.readString(file);
    assertThat(content).contains("Service.doWork");
    var output = out.toString();
    assertThat(output).contains("Service.doWork");
    assertThat(output).contains("Trace written: " + file);
  }

  @Test
  void writeTraceFileGeneratesJsonAndDiagramForMarkdown(@TempDir Path tempDir) throws Exception {
    var trace = traceWithOneCall();
    var out = new ByteArrayOutputStream();
    var mermaid = stubRenderer("sequenceDiagram\n  Test->>Service: doWork");

    TraceTestSupport.writeTraceFile(
        "com.example.FooTest",
        "testSomething",
        "test something",
        trace,
        false,
        tempDir,
        new PrintStream(out),
        "markdown",
        mermaid,
        null);

    assertThat(tempDir.resolve("traces/FooTest/test_something.json")).exists();
    assertThat(tempDir.resolve("diagrams/FooTest/test_something.mmd")).exists();
  }

  @Test
  void writeTraceFileSkipsWhenTraceIsEmpty(@TempDir Path tempDir) throws Exception {
    var out = new ByteArrayOutputStream();

    TraceTestSupport.writeTraceFile(
        "com.example.FooTest",
        "testSomething",
        "test something",
        emptyTrace(),
        false,
        tempDir,
        new PrintStream(out),
        "markdown",
        stubRenderer(""),
        null);

    assertThat(tempDir.resolve("traces")).doesNotExist();
    assertThat(out.toString()).isEmpty();
  }

  @Test
  void writeTraceFileWritesTextFormat(@TempDir Path tempDir) throws Exception {
    var trace = traceWithOneCall();
    var out = new ByteArrayOutputStream();

    TraceTestSupport.writeTraceFile(
        "com.example.FooTest",
        "testSomething",
        "test something",
        trace,
        false,
        tempDir,
        new PrintStream(out),
        "text",
        stubRenderer(""),
        null);

    var file = tempDir.resolve("traces/FooTest/test_something.txt");
    assertThat(file).exists();
    assertThat(Files.readString(file)).contains("Service.doWork");
  }

  @Test
  void writeClarityReportWritesFile(@TempDir Path tempDir) throws Exception {
    var traces = new LinkedHashMap<String, TraceTree>();
    traces.put("Customer places order", traceWithOneCall());

    TraceTestSupport.writeClarityReport(traces, tempDir, t -> "# Clarity Report\nscored");

    var reportFile = tempDir.resolve("clarity-report.md");
    assertThat(reportFile).exists();
    assertThat(Files.readString(reportFile)).contains("Clarity Report");
  }

  @Test
  void writeClarityReportSkipsWhenEmpty(@TempDir Path tempDir) throws Exception {
    TraceTestSupport.writeClarityReport(new LinkedHashMap<>(), tempDir, t -> "report");

    assertThat(tempDir.resolve("clarity-report.md")).doesNotExist();
  }

  @Test
  void printConsoleSummaryOutputsSuiteFooter() {
    var traces = new LinkedHashMap<String, TraceTree>();
    traces.put("Customer places order", traceWithOneCall());
    traces.put("Customer cancels order", traceWithOneCall());
    var out = new ByteArrayOutputStream();

    TraceTestSupport.printConsoleSummary(
        traces, Path.of("build/narrativetrace"), new PrintStream(out), tree -> 0.85);

    var output = out.toString();
    assertThat(output).contains("NarrativeTrace — Suite complete");
    assertThat(output).contains("2 scenarios recorded");
    assertThat(output).contains("Clarity:");
  }

  @Test
  void existingWriteClarityReportDoesNotWriteJson(@TempDir Path tempDir) throws Exception {
    var traces = new LinkedHashMap<String, TraceTree>();
    traces.put("Scenario", traceWithOneCall());

    TraceTestSupport.writeClarityReport(traces, tempDir, t -> "# Report");

    assertThat(tempDir.resolve("clarity-report.md")).exists();
    assertThat(tempDir.resolve("clarity-results.json")).doesNotExist();
  }

  @Test
  void writeClarityReportAlsoWritesJson(@TempDir Path tempDir) throws Exception {
    var traces = new LinkedHashMap<String, TraceTree>();
    traces.put("Customer places order", traceWithOneCall());

    TraceTestSupport.writeClarityReport(
        traces, tempDir, t -> "# Report", t -> "{\"version\":\"1.0\"}");

    assertThat(tempDir.resolve("clarity-report.md")).exists();
    assertThat(tempDir.resolve("clarity-results.json")).exists();
    assertThat(Files.readString(tempDir.resolve("clarity-results.json")))
        .isEqualTo("{\"version\":\"1.0\"}");
  }

  private static ai.narrativetrace.core.render.NarrativeRenderer stubRenderer(String output) {
    return tree -> output;
  }
}
