package ai.narrativetrace.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.clarity.ClarityAnalyzer;
import ai.narrativetrace.clarity.ClarityReportRenderer;
import ai.narrativetrace.clarity.ClarityResult;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.output.TraceTestSupport;
import ai.narrativetrace.core.render.NarrativeRenderer;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import ai.narrativetrace.core.tree.TraceTree;
import ai.narrativetrace.diagrams.MermaidSequenceDiagramRenderer;
import ai.narrativetrace.diagrams.PlantUmlSequenceDiagramRenderer;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(NarrativeTraceExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NarrativeTraceExtensionTest {

  @Test
  void providesNarrativeContextViaParameterResolution(NarrativeContext context) {
    assertThat(context).isNotNull();
  }

  @Test
  @Order(1)
  void firstTestPopulatesTrace(NarrativeContext context) {
    context.enterMethod(new MethodSignature("A", "first", List.of()));
    context.exitMethodWithReturn("done");
    assertThat(context.captureTrace().roots()).hasSize(1);
  }

  @Test
  @Order(2)
  void secondTestStartsWithEmptyTrace(NarrativeContext context) {
    assertThat(context.captureTrace().isEmpty()).isTrue();
  }

  @Test
  void supportsParameterReturnsFalseForNonContextType() throws Exception {
    var extension = new NarrativeTraceExtension();
    var param =
        getClass().getDeclaredMethod("methodWithStringParam", String.class).getParameters()[0];
    var paramContext = new StubParameterContext(param);

    assertThat(extension.supportsParameter(paramContext, null)).isFalse();
  }

  @SuppressWarnings("unused")
  private void methodWithStringParam(String value) {}

  @Test
  void handleAfterTestPrintsFailureReportWhenTestFailsWithTrace() {
    var trace = traceWithOneCall();
    var out = new ByteArrayOutputStream();

    new NarrativeTraceExtension()
        .handleAfterTest(
            "customer places order",
            true,
            trace,
            new AssertionError("expected true"),
            new PrintStream(out));

    var output = out.toString();
    assertThat(output).contains("Scenario: customer places order");
    assertThat(output).contains("Service.doWork");
  }

  @Test
  void handleAfterTestDoesNothingWhenTraceIsEmpty() {
    var out = new ByteArrayOutputStream();

    new NarrativeTraceExtension()
        .handleAfterTest(
            "test fails", true, emptyTrace(), new AssertionError("nope"), new PrintStream(out));

    assertThat(out.toString()).isEmpty();
  }

  @Test
  void handleAfterTestDoesNothingWhenTestPasses() {
    var trace = traceWithOneCall();
    var out = new ByteArrayOutputStream();

    new NarrativeTraceExtension()
        .handleAfterTest("test passes", false, trace, null, new PrintStream(out));

    assertThat(out.toString()).isEmpty();
  }

  @Test
  void writeTraceFileWritesMarkdownAndPrintsPath(@TempDir Path tempDir) throws Exception {
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
        "markdown",
        mermaid(),
        plantuml());

    var file = tempDir.resolve("traces/FooTest/test_something.md");
    assertThat(file).exists();
    var content = java.nio.file.Files.readString(file);
    assertThat(content).contains("Service.doWork");
    assertThat(content).contains("test something");
    var output = out.toString();
    assertThat(output).contains("Service.doWork");
    assertThat(output).doesNotContain("---\ntype: trace");
    assertThat(output).contains("Trace written: " + file);
  }

  @Test
  void markdownTraceContainsHumanizedScenarioName(@TempDir Path tempDir) throws Exception {
    var trace = traceWithOneCall();
    var out = new ByteArrayOutputStream();

    TraceTestSupport.writeTraceFile(
        "com.example.OrderServiceTest",
        "customerPlacesOrder",
        "customerPlacesOrder()",
        trace,
        false,
        tempDir,
        new PrintStream(out),
        "markdown",
        mermaid(),
        plantuml());

    var file = tempDir.resolve("traces/OrderServiceTest/customer_places_order.md");
    assertThat(file).exists();
    var content = java.nio.file.Files.readString(file);
    assertThat(content).contains("Customer places order");
    assertThat(content).doesNotContain("customerPlacesOrder()");
  }

  @Test
  void writeTraceFileWritesTextWhenFormatIsText(@TempDir Path tempDir) throws Exception {
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
        mermaid(),
        plantuml());

    var file = tempDir.resolve("traces/FooTest/test_something.txt");
    assertThat(file).exists();
    var content = java.nio.file.Files.readString(file);
    assertThat(content).contains("Service.doWork");
    assertThat(content).doesNotContain("**Service.doWork**");
  }

  @Test
  void writeTraceFileWritesMermaidWhenFormatIsMermaid(@TempDir Path tempDir) throws Exception {
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
        "mermaid",
        mermaid(),
        plantuml());

    var file = tempDir.resolve("traces/FooTest/test_something.mmd");
    assertThat(file).exists();
    var content = java.nio.file.Files.readString(file);
    assertThat(content).startsWith("sequenceDiagram");
    assertThat(content).contains("Service");
  }

  @Test
  void writeTraceFileWritesPlantUmlWhenFormatIsPlantUml(@TempDir Path tempDir) throws Exception {
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
        "plantuml",
        mermaid(),
        plantuml());

    var file = tempDir.resolve("traces/FooTest/test_something.puml");
    assertThat(file).exists();
    var content = java.nio.file.Files.readString(file);
    assertThat(content).startsWith("@startuml");
    assertThat(content).contains("Service");
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
        mermaid(),
        plantuml());

    assertThat(tempDir.resolve("traces")).doesNotExist();
    assertThat(out.toString()).isEmpty();
  }

  @Test
  void afterTestExecutionPrintsFailureReportForFailingTest() {
    var oldOut = System.out;
    var captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured));
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          FailingTestFixture.class))
                  .build());
    } finally {
      System.setOut(oldOut);
    }

    var output = captured.toString();
    assertThat(output).contains("Scenario: failing test");
    assertThat(output).contains("Service.doWork");
  }

  @Test
  void outputSystemPropertyEnablesFileWriting(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          FailingTestFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    var traceFile = tempDir.resolve("traces/FailingTestFixture/failing_test.md");
    assertThat(traceFile).exists();
    try {
      assertThat(java.nio.file.Files.readString(traceFile)).contains("Service.doWork");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void outputDirSystemPropertyOverridesDefault(@TempDir Path tempDir) {
    var customDir = tempDir.resolve("custom-output");
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", customDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          FailingTestFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    assertThat(customDir.resolve("traces/FailingTestFixture/failing_test.md")).exists();
  }

  @Test
  void producesFullOutputDirectoryStructure(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          MultiTestFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    // Markdown traces
    assertThat(tempDir.resolve("traces/MultiTestFixture/customer_places_order.md")).exists();
    assertThat(tempDir.resolve("traces/MultiTestFixture/customer_cancels_order.md")).exists();

    // JSON exports
    assertThat(tempDir.resolve("traces/MultiTestFixture/customer_places_order.json")).exists();
    assertThat(tempDir.resolve("traces/MultiTestFixture/customer_cancels_order.json")).exists();

    // Mermaid diagrams
    assertThat(tempDir.resolve("diagrams/MultiTestFixture/customer_places_order.mmd")).exists();
    assertThat(tempDir.resolve("diagrams/MultiTestFixture/customer_cancels_order.mmd")).exists();

    // Suite-level clarity report
    assertThat(tempDir.resolve("clarity-report.md")).exists();
  }

  @Test
  void printsConsoleSummaryAfterAllTests(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    var oldOut = System.out;
    var captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured));
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          MultiTestFixture.class))
                  .build());
    } finally {
      System.setOut(oldOut);
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    var output = captured.toString();
    assertThat(output).contains("NarrativeTrace — Suite complete");
    assertThat(output).contains("2 scenarios recorded");
    assertThat(output).contains("Clarity:");
  }

  @Test
  void generatesClarityReportAfterAllTests(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          MultiTestFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    var reportFile = tempDir.resolve("clarity-report.md");
    assertThat(reportFile).exists();
    try {
      var content = java.nio.file.Files.readString(reportFile);
      assertThat(content).contains("customer places order");
      assertThat(content).contains("customer cancels order");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void afterAllWritesClarityJson(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          MultiTestFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    var jsonFile = tempDir.resolve("clarity-results.json");
    assertThat(jsonFile).exists();
    try {
      var content = java.nio.file.Files.readString(jsonFile);
      assertThat(content).contains("\"version\":\"1.0\"");
      assertThat(content).contains("\"scenarios\":");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void clarityJsonContainsExpectedStructure(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          MultiTestFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    try {
      var content = java.nio.file.Files.readString(tempDir.resolve("clarity-results.json"));
      assertThat(content).contains("\"version\":\"1.0\"");
      assertThat(content).contains("\"overallScore\":");
      assertThat(content).contains("\"methodNameScore\":");
      assertThat(content).contains("\"classNameScore\":");
      assertThat(content).contains("\"parameterNameScore\":");
      assertThat(content).contains("\"structuralScore\":");
      assertThat(content).contains("\"cohesionScore\":");
      assertThat(content).contains("\"issues\":");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void combinedReportContainsScenariosFromMultipleClasses(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          MultiTestFixture.class),
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          SecondMultiTestFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    var jsonFile = tempDir.resolve("clarity-results.json");
    assertThat(jsonFile).exists();
    try {
      var content = java.nio.file.Files.readString(jsonFile);
      assertThat(content).contains("customer places order");
      assertThat(content).contains("customer cancels order");
      assertThat(content).contains("customer checks inventory");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }

    var reportFile = tempDir.resolve("clarity-report.md");
    assertThat(reportFile).exists();
    try {
      var content = java.nio.file.Files.readString(reportFile);
      assertThat(content).contains("customer places order");
      assertThat(content).contains("customer checks inventory");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void globalAccumulatorCloseDoesNothingWhenEmpty(@TempDir Path tempDir) {
    var accumulator = new NarrativeTraceExtension.GlobalTraceAccumulator();
    accumulator.close();

    assertThat(tempDir.resolve("clarity-report.md")).doesNotExist();
  }

  @Test
  void serviceLoaderDiscoversExtension() {
    var extensions = ServiceLoader.load(Extension.class);
    var found = false;
    for (var ext : extensions) {
      if (ext instanceof NarrativeTraceExtension) {
        found = true;
        break;
      }
    }
    assertThat(found).as("ServiceLoader should discover NarrativeTraceExtension").isTrue();
  }

  @Test
  void handleAfterTestPrintsTemplateWarningsWhenUnresolvedPlaceholders() {
    var sig =
        new MethodSignature(
            "OrderService", "placeOrder", List.of(), "Placing order for {custmerId}", null);
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("\"ok\""));
    var trace = new DefaultTraceTree(List.of(node));
    var out = new ByteArrayOutputStream();

    new NarrativeTraceExtension()
        .handleAfterTest("places order", false, trace, null, new PrintStream(out));

    var output = out.toString();
    assertThat(output).contains("Unresolved template placeholder");
    assertThat(output).contains("OrderService.placeOrder");
    assertThat(output).contains("{custmerId}");
  }

  @Test
  void handleAfterTestNoWarningsForCleanTrace() {
    var sig =
        new MethodSignature(
            "OrderService", "placeOrder", List.of(), "Placing order for C-123", null);
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("\"ok\""));
    var trace = new DefaultTraceTree(List.of(node));
    var out = new ByteArrayOutputStream();

    new NarrativeTraceExtension()
        .handleAfterTest("places order", false, trace, null, new PrintStream(out));

    assertThat(out.toString()).isEmpty();
  }

  @Test
  void writeTraceFileAlsoGeneratesJsonExport(@TempDir Path tempDir) throws Exception {
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
        "markdown",
        mermaid(),
        plantuml());

    var jsonFile = tempDir.resolve("traces/FooTest/test_something.json");
    assertThat(jsonFile).exists();
    var content = java.nio.file.Files.readString(jsonFile);
    assertThat(content).contains("\"scenario\"");
    assertThat(content).contains("\"test something\"");
    assertThat(content).contains("\"Service\"");
    assertThat(content).contains("\"doWork\"");
  }

  @Test
  void writeTraceFileAlsoGeneratesMermaidDiagram(@TempDir Path tempDir) throws Exception {
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
        "markdown",
        mermaid(),
        plantuml());

    var diagram = tempDir.resolve("diagrams/FooTest/test_something.mmd");
    assertThat(diagram).exists();
    var content = java.nio.file.Files.readString(diagram);
    assertThat(content).startsWith("sequenceDiagram");
    assertThat(content).contains("Service");
  }

  @Test
  void writesClarityReportFromAccumulatedTraces(@TempDir Path tempDir) throws Exception {
    var traces = new LinkedHashMap<String, TraceTree>();
    traces.put("Customer places order", traceWithOneCall());
    traces.put("Customer cancels order", traceWithOneCall());

    TraceTestSupport.writeClarityReport(traces, tempDir, this::renderClarityReport);

    var reportFile = tempDir.resolve("clarity-report.md");
    assertThat(reportFile).exists();
    var content = java.nio.file.Files.readString(reportFile);
    assertThat(content).contains("Customer places order");
    assertThat(content).contains("Customer cancels order");
  }

  @Test
  void writeTraceFileIoErrorIsCaughtGracefully(@TempDir Path tempDir) throws Exception {
    // Create a file where a directory is expected, causing IOException on write
    var blockingFile = tempDir.resolve("traces");
    java.nio.file.Files.writeString(blockingFile, "blocker");

    var oldErr = System.err;
    var capturedErr = new ByteArrayOutputStream();
    System.setErr(new PrintStream(capturedErr));
    try {
      System.setProperty("narrativetrace.output", "true");
      System.setProperty("narrativetrace.outputDir", tempDir.toString());
      try {
        org.junit.platform.launcher.core.LauncherFactory.create()
            .execute(
                org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                    .selectors(
                        org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                            IoErrorFixture.class))
                    .build());
      } finally {
        System.clearProperty("narrativetrace.output");
        System.clearProperty("narrativetrace.outputDir");
      }
    } finally {
      System.setErr(oldErr);
    }

    assertThat(capturedErr.toString()).contains("Failed to write trace file:");
  }

  @Test
  void afterAllCatchesIoErrorOnClarityReportWrite(@TempDir Path tempDir) throws Exception {
    // Create a file where the clarity-report.md directory should go
    java.nio.file.Files.writeString(tempDir.resolve("clarity-report.md"), "blocker");
    // Make it a directory to block the file write
    java.nio.file.Files.delete(tempDir.resolve("clarity-report.md"));
    java.nio.file.Files.createDirectories(tempDir.resolve("clarity-report.md"));

    var oldErr = System.err;
    var capturedErr = new ByteArrayOutputStream();
    System.setErr(new PrintStream(capturedErr));
    try {
      System.setProperty("narrativetrace.output", "true");
      System.setProperty("narrativetrace.outputDir", tempDir.toString());
      try {
        org.junit.platform.launcher.core.LauncherFactory.create()
            .execute(
                org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                    .selectors(
                        org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                            MultiTraceIoErrorFixture.class))
                    .build());
      } finally {
        System.clearProperty("narrativetrace.output");
        System.clearProperty("narrativetrace.outputDir");
      }
    } finally {
      System.setErr(oldErr);
    }

    assertThat(capturedErr.toString()).contains("Failed to write clarity report:");
  }

  @Test
  void afterAllSkipsWhenNoTracesAccumulated(@TempDir Path tempDir) {
    System.setProperty("narrativetrace.output", "true");
    System.setProperty("narrativetrace.outputDir", tempDir.toString());
    try {
      org.junit.platform.launcher.core.LauncherFactory.create()
          .execute(
              org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                  .selectors(
                      org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                          EmptyTraceFixture.class))
                  .build());
    } finally {
      System.clearProperty("narrativetrace.output");
      System.clearProperty("narrativetrace.outputDir");
    }

    assertThat(tempDir.resolve("clarity-report.md")).doesNotExist();
  }

  @Test
  void writeTraceFileHandlesUnqualifiedClassName(@TempDir Path tempDir) throws Exception {
    var trace = traceWithOneCall();
    var out = new ByteArrayOutputStream();

    TraceTestSupport.writeTraceFile(
        "FooTest",
        "testSomething",
        "test something",
        trace,
        false,
        tempDir,
        new PrintStream(out),
        "markdown",
        mermaid(),
        plantuml());

    var diagramFile = tempDir.resolve("diagrams/FooTest/test_something.mmd");
    assertThat(diagramFile).exists();
  }

  @Test
  void writeClarityReportSkipsWhenTracesAreEmpty(@TempDir Path tempDir) throws Exception {
    TraceTestSupport.writeClarityReport(new LinkedHashMap<>(), tempDir, t -> "report");

    assertThat(tempDir.resolve("clarity-report.md")).doesNotExist();
  }

  @Test
  void readsConfigViaConfigurationParameterInsteadOfSystemProperty(@TempDir Path tempDir) {
    // Ensure no system properties are set
    System.clearProperty("narrativetrace.output");
    System.clearProperty("narrativetrace.outputDir");
    System.clearProperty("narrativetrace.format");

    org.junit.platform.launcher.core.LauncherFactory.create()
        .execute(
            org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                .selectors(
                    org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(
                        FailingTestFixture.class))
                .configurationParameter("narrativetrace.output", "true")
                .configurationParameter("narrativetrace.outputDir", tempDir.toString())
                .configurationParameter("narrativetrace.format", "markdown")
                .build());

    var traceFile = tempDir.resolve("traces/FailingTestFixture/failing_test.md");
    assertThat(traceFile).exists();
  }

  private String renderClarityReport(Map<String, TraceTree> traces) {
    var analyzer = new ClarityAnalyzer();
    var results = new LinkedHashMap<String, ClarityResult>();
    for (var entry : traces.entrySet()) {
      results.put(entry.getKey(), analyzer.analyze(entry.getValue()));
    }
    return new ClarityReportRenderer().renderSuiteReport(results);
  }

  private static NarrativeRenderer mermaid() {
    return new MermaidSequenceDiagramRenderer()::render;
  }

  private static NarrativeRenderer plantuml() {
    return new PlantUmlSequenceDiagramRenderer()::render;
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
}
