package ai.narrativetrace.junit4;

import ai.narrativetrace.clarity.ClarityAnalyzer;
import ai.narrativetrace.clarity.ClarityJsonExporter;
import ai.narrativetrace.clarity.ClarityReportRenderer;
import ai.narrativetrace.clarity.ClarityResult;
import ai.narrativetrace.core.output.TraceTestSupport;
import ai.narrativetrace.core.tree.TraceTree;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NarrativeTraceClassRule implements TestRule {

    private static final Map<String, TraceTree> GLOBAL_TRACES = Collections.synchronizedMap(new LinkedHashMap<>());

    private final Map<String, TraceTree> accumulatedTraces = new LinkedHashMap<>();
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    public NarrativeTraceRule testRule() {
        var rule = new NarrativeTraceRule();
        rule.setClassRule(this);
        return rule;
    }

    void accumulate(String scenario, TraceTree trace) {
        accumulatedTraces.put(scenario, trace);
    }

    Map<String, TraceTree> accumulatedTraces() {
        return accumulatedTraces;
    }

    void setOut(PrintStream out) {
        this.out = out;
    }

    void setErr(PrintStream err) {
        this.err = err;
    }

    static void resetGlobalAccumulator() {
        GLOBAL_TRACES.clear();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                afterAll();
            }
        };
    }

    private void afterAll() {
        if (!NarrativeTraceRule.isOutputEnabled()) {
            return;
        }
        if (accumulatedTraces.isEmpty()) {
            return;
        }
        GLOBAL_TRACES.putAll(accumulatedTraces);
        var outputDir = Path.of(System.getProperty("narrativetrace.outputDir", "build/narrativetrace"));
        try {
            TraceTestSupport.writeClarityReport(GLOBAL_TRACES, outputDir,
                    this::renderClarityReport, this::exportClarityJson);
        } catch (IOException e) {
            err.println("Failed to write clarity report: " + e.getMessage());
        }
        TraceTestSupport.printConsoleSummary(GLOBAL_TRACES, outputDir, out,
                tree -> new ClarityAnalyzer().analyze(tree).overallScore());
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
}
