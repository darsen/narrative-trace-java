package ai.narrativetrace.core.output;

import ai.narrativetrace.core.export.JsonExporter;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import ai.narrativetrace.core.render.MarkdownRenderer;
import ai.narrativetrace.core.render.NarrativeRenderer;
import ai.narrativetrace.core.render.TraceMetadata;
import ai.narrativetrace.core.tree.TraceTree;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

public final class TraceTestSupport {

    private TraceTestSupport() {
    }

    public static String extensionForFormat(String format) {
        return switch (format.toLowerCase()) {
            case "text" -> ".txt";
            case "mermaid" -> ".mmd";
            case "plantuml" -> ".puml";
            default -> ".md";
        };
    }

    public static String buildFailureReport(String scenario, String trace) {
        return "\n\n" + scenario + "\n\nExecution trace:\n" + trace;
    }

    public static String renderForFormat(String format, TraceTree trace, String displayName, boolean failed,
                                         NarrativeRenderer mermaidRenderer, NarrativeRenderer plantumlRenderer) {
        var scenario = ScenarioFramer.humanize(displayName);
        return switch (format.toLowerCase()) {
            case "text" -> ScenarioFramer.frame(displayName) + "\n\n" + new IndentedTextRenderer().render(trace);
            case "mermaid" -> mermaidRenderer.render(trace);
            case "plantuml" -> plantumlRenderer.render(trace);
            default -> {
                var metadata = new TraceMetadata(scenario, failed ? "FAILED" : "PASSED");
                yield new MarkdownRenderer().renderDocument(trace, metadata);
            }
        };
    }

    public static void writeTraceFile(String testClassName, String testMethodName, String displayName,
                                      TraceTree trace, boolean failed, Path outputDir, PrintStream out,
                                      String format, NarrativeRenderer mermaidRenderer,
                                      NarrativeRenderer plantumlRenderer) throws IOException {
        if (trace.isEmpty()) {
            return;
        }
        var resolver = new OutputDirectoryResolver(outputDir);
        var mdFile = resolver.traceFile(testClassName, testMethodName);
        var baseName = mdFile.getFileName().toString().replaceAll("\\.md$", "");
        var file = mdFile.resolveSibling(baseName + extensionForFormat(format));
        var content = renderForFormat(format, trace, displayName, failed, mermaidRenderer, plantumlRenderer);
        var writer = new TraceFileWriter();
        writer.write(content, file);
        out.println("\n" + ScenarioFramer.frame(displayName) + "\n\n" + new IndentedTextRenderer().render(trace));
        out.println("Trace written: " + file);

        if ("markdown".equalsIgnoreCase(format)) {
            writeMarkdownExtras(writer, testClassName, displayName, trace, failed,
                    outputDir, mdFile, baseName, mermaidRenderer);
        }
    }

    private static void writeMarkdownExtras(TraceFileWriter writer, String testClassName,
                                            String displayName, TraceTree trace, boolean failed,
                                            Path outputDir, Path mdFile, String baseName,
                                            NarrativeRenderer mermaidRenderer) throws IOException {
        var simpleName = testClassName.contains(".")
                ? testClassName.substring(testClassName.lastIndexOf('.') + 1) : testClassName;
        var diagramFile = outputDir.resolve("diagrams").resolve(simpleName).resolve(baseName + ".mmd");
        writer.write(mermaidRenderer.render(trace), diagramFile);

        var scenario = ScenarioFramer.humanize(displayName);
        var metadata = new TraceMetadata(scenario, failed ? "FAILED" : "PASSED");
        var jsonFile = mdFile.resolveSibling(baseName + ".json");
        writer.write(new JsonExporter().exportDocument(trace, metadata), jsonFile);
    }

    public static void writeClarityReport(Map<String, TraceTree> traces, Path outputDir,
                                          Function<Map<String, TraceTree>, String> reportRenderer) throws IOException {
        if (traces.isEmpty()) {
            return;
        }
        var report = reportRenderer.apply(traces);
        new TraceFileWriter().write(report, outputDir.resolve("clarity-report.md"));
    }

    public static void writeClarityReport(Map<String, TraceTree> traces, Path outputDir,
                                          Function<Map<String, TraceTree>, String> reportRenderer,
                                          Function<Map<String, TraceTree>, String> jsonExporter) throws IOException {
        writeClarityReport(traces, outputDir, reportRenderer);
        if (traces.isEmpty()) {
            return;
        }
        var json = jsonExporter.apply(traces);
        new TraceFileWriter().write(json, outputDir.resolve("clarity-results.json"));
    }

    public static void printConsoleSummary(Map<String, TraceTree> traces, Path outputDir, PrintStream out,
                                           Function<TraceTree, Double> clarityScorer) {
        var scores = new ArrayList<Double>();
        for (var tree : traces.values()) {
            scores.add(clarityScorer.apply(tree));
        }
        var reporter = new ConsoleSummaryReporter();
        out.println(reporter.formatSuiteFooter(traces.size(), outputDir.toString(), scores));
    }
}
