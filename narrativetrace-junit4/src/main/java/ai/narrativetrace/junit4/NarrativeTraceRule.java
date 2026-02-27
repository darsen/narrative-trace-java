package ai.narrativetrace.junit4;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.output.ScenarioFramer;
import ai.narrativetrace.core.output.TemplateWarningCollector;
import ai.narrativetrace.core.output.TraceTestSupport;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import ai.narrativetrace.core.render.NarrativeRenderer;
import ai.narrativetrace.diagrams.MermaidSequenceDiagramRenderer;
import ai.narrativetrace.diagrams.PlantUmlSequenceDiagramRenderer;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * JUnit 4 per-test rule that captures traces for individual test methods.
 *
 * <p>Use as a {@code @Rule} field in your test class. Creates a fresh {@link
 * ThreadLocalNarrativeContext} before each test and provides it via {@link #context()}.
 *
 * <p>For class-level output (clarity reports, console summaries), pair with {@link
 * NarrativeTraceClassRule}:
 *
 * <pre>{@code
 * public class OrderServiceTest {
 *     @ClassRule public static NarrativeTraceClassRule classRule = new NarrativeTraceClassRule();
 *     @Rule public NarrativeTraceRule rule = classRule.testRule();
 *
 *     @Test
 *     public void placesOrder() {
 *         var service = NarrativeTraceProxy.trace(impl, OrderService.class, rule.context());
 *         service.placeOrder("item-1", 3);
 *     }
 * }
 * }</pre>
 *
 * @see NarrativeTraceClassRule
 * @see ai.narrativetrace.core.context.NarrativeContext
 */
public class NarrativeTraceRule extends TestWatcher {

  private NarrativeContext context;
  private boolean testFailed;
  private NarrativeTraceClassRule classRule;
  private PrintStream out = System.out;
  private PrintStream err = System.err;
  private NarrativeRenderer mermaidRenderer = new MermaidSequenceDiagramRenderer()::render;
  private NarrativeRenderer plantumlRenderer = new PlantUmlSequenceDiagramRenderer()::render;

  /**
   * Returns the {@link NarrativeContext} for the current test. Use this to create tracing proxies
   * via {@link ai.narrativetrace.proxy.NarrativeTraceProxy#trace}.
   *
   * @return the current test's narrative context
   */
  public NarrativeContext context() {
    return context;
  }

  void setClassRule(NarrativeTraceClassRule classRule) {
    this.classRule = classRule;
  }

  void setOut(PrintStream out) {
    this.out = out;
  }

  void setErr(PrintStream err) {
    this.err = err;
  }

  void setMermaidRenderer(NarrativeRenderer mermaidRenderer) {
    this.mermaidRenderer = mermaidRenderer;
  }

  void setPlantumlRenderer(NarrativeRenderer plantumlRenderer) {
    this.plantumlRenderer = plantumlRenderer;
  }

  @Override
  protected void starting(Description description) {
    context = new ThreadLocalNarrativeContext();
    testFailed = false;
  }

  @Override
  protected void failed(Throwable e, Description description) {
    testFailed = true;
    var trace = context.captureTrace();
    if (trace.isEmpty()) {
      return;
    }
    var scenario = ScenarioFramer.frame(description.getMethodName());
    var rendered = new IndentedTextRenderer().render(trace);
    out.println(TraceTestSupport.buildFailureReport(scenario, rendered));
  }

  @Override
  protected void finished(Description description) {
    if (context == null) {
      return;
    }
    printTemplateWarnings();
    writeTraceIfEnabled(description);
    accumulateIfLinked(description);
  }

  private void printTemplateWarnings() {
    var trace = context.captureTrace();
    if (trace.isEmpty()) {
      return;
    }
    var warnings = TemplateWarningCollector.collect(trace);
    var formatted = TemplateWarningCollector.format(warnings);
    if (!formatted.isEmpty()) {
      out.print(formatted);
    }
  }

  private void writeTraceIfEnabled(Description description) {
    if (!isOutputEnabled()) {
      return;
    }
    var trace = context.captureTrace();
    var outputDir = Path.of(System.getProperty("narrativetrace.outputDir", "build/narrativetrace"));
    var format = System.getProperty("narrativetrace.format", "markdown");
    var testClassName =
        description.getTestClass() != null
            ? description.getTestClass().getName()
            : description.getClassName();
    var testMethodName = description.getMethodName();
    try {
      TraceTestSupport.writeTraceFile(
          testClassName,
          testMethodName,
          testMethodName,
          trace,
          testFailed,
          outputDir,
          out,
          format,
          mermaidRenderer,
          plantumlRenderer);
    } catch (IOException ex) {
      err.println("Failed to write trace file: " + ex.getMessage());
    }
  }

  private void accumulateIfLinked(Description description) {
    if (classRule == null) {
      return;
    }
    var trace = context.captureTrace();
    if (!trace.isEmpty()) {
      var scenario = ScenarioFramer.humanize(description.getMethodName());
      classRule.accumulate(scenario, trace);
    }
  }

  static boolean isOutputEnabled() {
    return "true".equalsIgnoreCase(System.getProperty("narrativetrace.output", "false"));
  }
}
