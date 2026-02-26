package ai.narrativetrace.core.export;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.render.TraceMetadata;
import ai.narrativetrace.core.tree.TraceTree;

import java.util.stream.Collectors;

public final class JsonExporter {

    public String exportDocument(TraceTree tree, TraceMetadata metadata) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": \"1.0\",\n");
        sb.append("  \"scenario\": {\n");
        sb.append("    \"name\": \"").append(escapeJson(metadata.scenario())).append("\",\n");
        sb.append("    \"result\": \"").append(escapeJson(metadata.result())).append("\"");
        if (!tree.roots().isEmpty()) {
            long durationMs = tree.roots().get(0).durationMillis();
            sb.append(",\n    \"durationMs\": ").append(durationMs);
        }
        sb.append("\n");
        sb.append("  },\n");
        sb.append("  \"events\": [\n");
        var ctx = new EmitContext();
        for (var root : tree.roots()) {
            flattenNode(root, 0, null, ctx, sb);
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    public String export(TraceTree tree) {
        var sb = new StringBuilder();
        sb.append("{\n  \"events\": [\n");
        var ctx = new EmitContext();
        for (var root : tree.roots()) {
            flattenNode(root, 0, null, ctx, sb);
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private void flattenNode(TraceNode node, int depth, Integer parentId,
                             EmitContext ctx, StringBuilder sb) {
        int id = ctx.nextId();
        appendEnterEvent(node, id, depth, parentId, ctx, sb);

        for (var child : node.children()) {
            flattenNode(child, depth + 1, id, ctx, sb);
        }

        appendExitEvent(node, ctx.nextId(), depth, parentId, sb);
    }

    private void appendEnterEvent(TraceNode node, int id, int depth, Integer parentId,
                                  EmitContext ctx, StringBuilder sb) {
        var sig = node.signature();
        ctx.appendSeparator(sb);
        sb.append("    {\n");
        appendCommonFields(sig, id, "enter", sb);
        sb.append("      \"params\": {");
        var params = sig.parameters().stream()
                .map(this::renderParam)
                .collect(Collectors.joining(", "));
        sb.append(params).append("},\n");
        appendFooterFields(depth, parentId, sb);
        sb.append("    }");
    }

    private void appendExitEvent(TraceNode node, int id, int depth, Integer parentId,
                                 StringBuilder sb) {
        var sig = node.signature();
        sb.append(",\n    {\n");
        if (node.outcome() instanceof TraceOutcome.Returned r) {
            appendCommonFields(sig, id, "exit", sb);
            sb.append("      \"returnValue\": \"").append(escapeJson(r.renderedValue())).append("\",\n");
        } else if (node.outcome() instanceof TraceOutcome.Threw t) {
            appendCommonFields(sig, id, "error", sb);
            sb.append("      \"error\": {\n");
            sb.append("        \"type\": \"").append(t.exception().getClass().getSimpleName()).append("\",\n");
            sb.append("        \"message\": \"").append(escapeJson(t.exception().getMessage())).append("\"\n");
            sb.append("      },\n");
        }
        sb.append("      \"durationMs\": ").append(node.durationMillis()).append(",\n");
        appendFooterFields(depth, parentId, sb);
        sb.append("    }");
    }

    private void appendCommonFields(MethodSignature sig, int id, String type,
                                    StringBuilder sb) {
        sb.append("      \"id\": ").append(id).append(",\n");
        sb.append("      \"type\": \"").append(type).append("\",\n");
        sb.append("      \"class\": \"").append(sig.className()).append("\",\n");
        sb.append("      \"method\": \"").append(sig.methodName()).append("\",\n");
    }

    private void appendFooterFields(int depth, Integer parentId, StringBuilder sb) {
        sb.append("      \"depth\": ").append(depth).append(",\n");
        sb.append("      \"parentId\": ").append(parentId == null ? "null" : parentId).append("\n");
    }

    private String renderParam(ParameterCapture param) {
        if (param.redacted()) {
            return "\"" + param.name() + "\": \"[REDACTED]\"";
        }
        var value = param.renderedValue();
        if (value == null || value.isEmpty()) {
            return "\"" + param.name() + "\": null";
        }
        return "\"" + param.name() + "\": \"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class EmitContext {
        private int nextId = 1;
        private boolean first = true;

        int nextId() {
            return nextId++;
        }

        void appendSeparator(StringBuilder sb) {
            if (!first) sb.append(",\n");
            first = false;
        }
    }
}
