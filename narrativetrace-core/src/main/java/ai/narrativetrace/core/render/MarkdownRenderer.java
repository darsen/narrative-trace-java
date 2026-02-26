package ai.narrativetrace.core.render;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.TraceTree;

import java.util.stream.Collectors;

public final class MarkdownRenderer implements NarrativeRenderer {

    private final long slowThresholdMs;

    public MarkdownRenderer() {
        this(200);
    }

    public MarkdownRenderer(long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    @Override
    public String render(TraceTree tree) {
        var sb = new StringBuilder();
        for (var root : tree.roots()) {
            renderNode(root, 0, sb);
        }
        return sb.toString().stripTrailing();
    }

    public String renderDocument(TraceTree tree, TraceMetadata metadata) {
        var sb = new StringBuilder();
        sb.append(new FrontmatterBuilder().scenario(metadata.scenario()).build(tree));
        if (!tree.roots().isEmpty()) {
            var root = tree.roots().get(0);
            var sig = root.signature();
            sb.append("\n## Trace: ").append(sig.className()).append(".").append(sig.methodName()).append("\n\n");
            sb.append("**Scenario:** ").append(metadata.scenario()).append("\n");
            long durationMs = root.durationMillis();
            sb.append("**Duration:** ").append(durationMs).append("ms | **Result:** ").append(metadata.result()).append("\n\n");
            sb.append("### Call Flow\n\n");
        }
        for (var root : tree.roots()) {
            renderNode(root, 0, sb);
        }
        return sb.toString().stripTrailing();
    }

    private void renderNode(TraceNode node, int depth, StringBuilder sb) {
        var indent = "  ".repeat(depth);
        var sig = node.signature();
        var params = sig.parameters().stream()
                .map(this::renderParam)
                .collect(Collectors.joining(", "));
        var methodCall = "**" + sig.className() + "." + sig.methodName() + "**(" + params + ")";

        if (node.children().isEmpty()) {
            sb.append(indent).append("- ").append(methodCall);
            renderOutcomeInline(node.outcome(), sig, depth, sb);
            renderDuration(node, sb);
            sb.append("\n");
        } else {
            sb.append(indent).append("- ").append(methodCall);
            renderDuration(node, sb);
            sb.append("\n");
            renderNarration(sig, indent, sb);
            for (var child : node.children()) {
                renderNode(child, depth + 1, sb);
            }
            sb.append(indent).append("  - ");
            renderOutcomeClosing(node.outcome(), sig, depth, sb);
            sb.append("\n");
        }
    }

    private void renderOutcomeInline(TraceOutcome outcome, MethodSignature sig, int depth, StringBuilder sb) {
        if (outcome instanceof TraceOutcome.Returned r) {
            sb.append(" → `").append(r.renderedValue()).append("`");
        } else if (outcome instanceof TraceOutcome.Threw t) {
            var errorIndent = "  ".repeat(depth + 1);
            sb.append("\n\n").append(errorIndent).append("> ❌ `").append(t.exception().getClass().getSimpleName())
                    .append("`: ").append(t.exception().getMessage());
            if (sig.errorContext() != null) {
                sb.append("\n").append(errorIndent).append("> ").append(sig.errorContext());
            }
        }
    }

    private void renderOutcomeClosing(TraceOutcome outcome, MethodSignature sig, int depth, StringBuilder sb) {
        if (outcome instanceof TraceOutcome.Returned r) {
            sb.append("→ `").append(r.renderedValue()).append("`");
        } else if (outcome instanceof TraceOutcome.Threw t) {
            var errorIndent = "  ".repeat(depth + 1);
            sb.append("❌ `").append(t.exception().getClass().getSimpleName())
                    .append("`: ").append(t.exception().getMessage());
            if (sig.errorContext() != null) {
                sb.append("\n").append(errorIndent).append("> ").append(sig.errorContext());
            }
        }
    }

    private void renderNarration(MethodSignature sig, String indent, StringBuilder sb) {
        if (sig.narration() != null) {
            sb.append(indent).append("  *").append(sig.narration()).append("*\n");
        }
    }

    private void renderDuration(TraceNode node, StringBuilder sb) {
        if (node.durationNanos() > 0) {
            long millis = node.durationMillis();
            sb.append(" — ").append(millis).append("ms");
            if (millis > slowThresholdMs) {
                sb.append(" ⚠️ slow");
            }
        }
    }

    private String renderParam(ParameterCapture param) {
        if (param.redacted()) {
            return param.name() + ": `[REDACTED]`";
        }
        return param.name() + ": `" + param.renderedValue() + "`";
    }
}
