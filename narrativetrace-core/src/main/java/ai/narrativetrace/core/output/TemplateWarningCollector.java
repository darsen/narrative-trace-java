package ai.narrativetrace.core.output;

import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.template.TemplateParser;
import ai.narrativetrace.core.tree.TraceTree;

import java.util.ArrayList;
import java.util.List;

public final class TemplateWarningCollector {

    public record TemplateWarning(String className, String methodName, String placeholder, String field) {
    }

    private TemplateWarningCollector() {
    }

    public static List<TemplateWarning> collect(TraceTree trace) {
        var warnings = new ArrayList<TemplateWarning>();
        for (var root : trace.roots()) {
            collectFromNode(root, warnings);
        }
        return List.copyOf(warnings);
    }

    public static String format(List<TemplateWarning> warnings) {
        if (warnings.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        sb.append("WARNING: Unresolved template placeholder(s) detected:\n");
        for (var w : warnings) {
            sb.append("  - ").append(w.className).append('.').append(w.methodName)
                    .append(": {").append(w.placeholder).append("} in ").append(w.field).append('\n');
        }
        return sb.toString();
    }

    private static void collectFromNode(TraceNode node, List<TemplateWarning> warnings) {
        var sig = node.signature();
        for (var placeholder : TemplateParser.findUnresolvedInResult(sig.narration())) {
            warnings.add(new TemplateWarning(sig.className(), sig.methodName(), placeholder, "narration"));
        }
        for (var placeholder : TemplateParser.findUnresolvedInResult(sig.errorContext())) {
            warnings.add(new TemplateWarning(sig.className(), sig.methodName(), placeholder, "errorContext"));
        }
        for (var child : node.children()) {
            collectFromNode(child, warnings);
        }
    }
}
