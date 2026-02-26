package ai.narrativetrace.junit5;

import ai.narrativetrace.clarity.ClarityAnalyzer;
import ai.narrativetrace.clarity.ClarityJsonExporter;
import ai.narrativetrace.clarity.ClarityReportRenderer;
import ai.narrativetrace.clarity.ClarityResult;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.output.ScenarioFramer;
import ai.narrativetrace.core.output.TemplateWarningCollector;
import ai.narrativetrace.core.output.TraceTestSupport;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import ai.narrativetrace.core.render.NarrativeRenderer;
import ai.narrativetrace.core.tree.TraceTree;
import ai.narrativetrace.diagrams.MermaidSequenceDiagramRenderer;
import ai.narrativetrace.diagrams.PlantUmlSequenceDiagramRenderer;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class NarrativeTraceExtension implements BeforeEachCallback, AfterTestExecutionCallback, AfterAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(NarrativeTraceExtension.class);
    private static final String CONTEXT_KEY = "narrativeContext";
    private static final String TRACES_KEY = "accumulatedTraces";
    private static final String GLOBAL_KEY = "globalTraceAccumulator";

    static class GlobalTraceAccumulator implements ExtensionContext.Store.CloseableResource {
        private final Map<String, TraceTree> allTraces = new LinkedHashMap<>();
        private Path outputDir;

        synchronized void contribute(Map<String, TraceTree> traces, Path outputDir) {
            allTraces.putAll(traces);
            if (this.outputDir == null) {
                this.outputDir = outputDir;
            }
        }

        @Override
        public void close() {
            if (allTraces.isEmpty()) {
                return;
            }
            var extension = new NarrativeTraceExtension();
            try {
                TraceTestSupport.writeClarityReport(allTraces, outputDir,
                        extension::renderClarityReport, extension::exportClarityJson);
            } catch (IOException e) {
                System.err.println("Failed to write clarity report: " + e.getMessage());
            }
            TraceTestSupport.printConsoleSummary(allTraces, outputDir, System.out,
                    tree -> new ClarityAnalyzer().analyze(tree).overallScore());
        }
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        var context = new ThreadLocalNarrativeContext();
        extensionContext.getStore(NAMESPACE).put(CONTEXT_KEY, context);
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) {
        var context = extensionContext.getStore(NAMESPACE).get(CONTEXT_KEY, NarrativeContext.class);
        if (context == null) {
            return;
        }
        var trace = context.captureTrace();
        var exception = extensionContext.getExecutionException().orElse(null);
        var failed = exception != null;
        var displayName = extensionContext.getDisplayName();

        handleAfterTest(displayName, failed, trace, exception, System.out);

        if ("true".equalsIgnoreCase(configParam(extensionContext, "narrativetrace.output", "false"))) {
            writeOutputAndAccumulate(extensionContext, displayName, trace, failed);
        }
    }

    private void writeOutputAndAccumulate(ExtensionContext extensionContext,
                                          String displayName, TraceTree trace, boolean failed) {
        var outputDir = Path.of(configParam(extensionContext, "narrativetrace.outputDir", "build/narrativetrace"));
        var format = configParam(extensionContext, "narrativetrace.format", "markdown");
        var testClassName = extensionContext.getRequiredTestClass().getName();
        var testMethodName = extensionContext.getRequiredTestMethod().getName();
        try {
            NarrativeRenderer mermaid = new MermaidSequenceDiagramRenderer()::render;
            NarrativeRenderer plantuml = new PlantUmlSequenceDiagramRenderer()::render;
            TraceTestSupport.writeTraceFile(testClassName, testMethodName, displayName, trace, failed,
                    outputDir, System.out, format, mermaid, plantuml);
        } catch (IOException e) {
            System.err.println("Failed to write trace file: " + e.getMessage());
        }

        if (!trace.isEmpty()) {
            var scenario = ScenarioFramer.humanize(displayName);
            var classStore = extensionContext.getParent().orElseThrow().getStore(NAMESPACE);
            @SuppressWarnings("unchecked")
            var accumulated = (Map<String, TraceTree>) classStore.getOrComputeIfAbsent(
                    TRACES_KEY, k -> new LinkedHashMap<String, TraceTree>(), Map.class);
            accumulated.put(scenario, trace);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        var outputEnabled = "true".equalsIgnoreCase(
                configParam(extensionContext, "narrativetrace.output", "false"));
        if (!outputEnabled) {
            return;
        }
        @SuppressWarnings("unchecked")
        var accumulated = (Map<String, TraceTree>) extensionContext.getStore(NAMESPACE).get(TRACES_KEY, Map.class);
        if (accumulated == null || accumulated.isEmpty()) {
            return;
        }
        var outputDir = Path.of(configParam(extensionContext, "narrativetrace.outputDir", "build/narrativetrace"));
        var rootStore = extensionContext.getRoot().getStore(NAMESPACE);
        var accumulator = rootStore.getOrComputeIfAbsent(
                GLOBAL_KEY, k -> new GlobalTraceAccumulator(), GlobalTraceAccumulator.class);
        accumulator.contribute(accumulated, outputDir);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == NarrativeContext.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(CONTEXT_KEY, NarrativeContext.class);
    }

    void handleAfterTest(String displayName, boolean failed, TraceTree trace, Throwable exception, java.io.PrintStream out) {
        if (!trace.isEmpty()) {
            var templateWarnings = TemplateWarningCollector.collect(trace);
            var formatted = TemplateWarningCollector.format(templateWarnings);
            if (!formatted.isEmpty()) {
                out.print(formatted);
            }
        }
        if (!failed || trace.isEmpty()) {
            return;
        }
        var scenario = ScenarioFramer.frame(displayName);
        var rendered = new IndentedTextRenderer().render(trace);
        out.println(TraceTestSupport.buildFailureReport(scenario, rendered));
    }

    private String renderClarityReport(Map<String, TraceTree> traces) {
        return new ClarityReportRenderer().renderSuiteReport(analyzeClarityResults(traces));
    }

    private String exportClarityJson(Map<String, TraceTree> traces) {
        return new ClarityJsonExporter().export(analyzeClarityResults(traces));
    }

    private Map<String, ClarityResult> analyzeClarityResults(Map<String, TraceTree> traces) {
        var analyzer = new ClarityAnalyzer();
        var results = new LinkedHashMap<String, ClarityResult>();
        for (var entry : traces.entrySet()) {
            results.put(entry.getKey(), analyzer.analyze(entry.getValue()));
        }
        return results;
    }

    private static String configParam(ExtensionContext context, String key, String defaultValue) {
        return context.getConfigurationParameter(key).orElse(defaultValue);
    }
}
