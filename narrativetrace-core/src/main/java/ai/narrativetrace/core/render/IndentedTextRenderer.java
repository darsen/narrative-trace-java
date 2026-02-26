package ai.narrativetrace.core.render;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.TraceTree;

import java.util.stream.Collectors;

public final class IndentedTextRenderer implements NarrativeRenderer {

    public IndentedTextRenderer() {
    }

    @Override
    public String render(TraceTree tree) {
        var sb = new StringBuilder();
        for (var root : tree.roots()) {
            renderNode(root, "", "", sb);
        }
        return sb.toString().stripTrailing();
    }

    private void renderNode(TraceNode node, String linePrefix, String contPrefix, StringBuilder sb) {
        var sig = node.signature();
        var params = sig.parameters().stream()
                .map(this::renderParam)
                .collect(Collectors.joining(", "));
        var header = sig.className() + "." + sig.methodName() + "(" + params + ")";

        if (node.children().isEmpty()) {
            sb.append(linePrefix).append(header);
            renderOutcomeInline(node.outcome(), sig, sb);
            renderDuration(node, sb);
            sb.append("\n");
        } else {
            sb.append(linePrefix).append(header).append("\n");
            renderNarration(sig, contPrefix, sb);
            var children = node.children();
            for (int i = 0; i < children.size(); i++) {
                var child = children.get(i);
                renderNode(child, contPrefix + "├── ", contPrefix + "│   ", sb);
            }
            sb.append(contPrefix).append("└── ");
            renderOutcomeClosing(node.outcome(), sig, sb);
            renderDuration(node, sb);
            sb.append("\n");
        }
    }

    private void renderOutcomeInline(TraceOutcome outcome, MethodSignature sig, StringBuilder sb) {
        if (outcome instanceof TraceOutcome.Returned r) {
            sb.append(" → ").append(r.renderedValue());
        } else if (outcome instanceof TraceOutcome.Threw t) {
            sb.append(" !! ").append(t.exception().getClass().getSimpleName())
                    .append(": ").append(t.exception().getMessage());
            if (sig.errorContext() != null) {
                sb.append(" | ").append(sig.errorContext());
            }
        }
    }

    private void renderOutcomeClosing(TraceOutcome outcome, MethodSignature sig, StringBuilder sb) {
        if (outcome instanceof TraceOutcome.Returned r) {
            sb.append("→ ").append(r.renderedValue());
        } else if (outcome instanceof TraceOutcome.Threw t) {
            sb.append("!! ").append(t.exception().getClass().getSimpleName())
                    .append(": ").append(t.exception().getMessage());
            if (sig.errorContext() != null) {
                sb.append(" | ").append(sig.errorContext());
            }
        }
    }

    private void renderNarration(MethodSignature sig, String contPrefix, StringBuilder sb) {
        if (sig.narration() != null) {
            sb.append(contPrefix).append("│   // ").append(sig.narration()).append("\n");
        }
    }

    private void renderDuration(TraceNode node, StringBuilder sb) {
        if (node.durationNanos() > 0) {
            long millis = node.durationMillis();
            sb.append(" — ").append(millis).append("ms");
        }
    }

    private String renderParam(ParameterCapture param) {
        if (param.redacted()) {
            return param.name() + ": [REDACTED]";
        }
        return param.name() + ": " + param.renderedValue();
    }
}
