package ai.narrativetrace.core.render;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.TraceTree;
import java.util.stream.Collectors;

/** Renders trace trees as natural-language prose sentences. */
public final class ProseRenderer implements NarrativeRenderer {

  public ProseRenderer() {}

  @Override
  public String render(TraceTree tree) {
    var sb = new StringBuilder();
    for (var root : tree.roots()) {
      renderNode(root, 0, sb);
    }
    return sb.toString().stripTrailing();
  }

  private void renderNode(TraceNode node, int depth, StringBuilder sb) {
    var indent = "  ".repeat(depth);
    var sig = node.signature();

    var subject = "The " + CamelCaseSplitter.toPhrase(sig.className());
    var action = CamelCaseSplitter.toPhrase(sig.methodName());
    var params = renderParams(node);

    sb.append(indent).append(subject).append(" ");

    boolean isError = node.outcome() instanceof TraceOutcome.Threw;
    if (isError) {
      sb.append("failed to ");
    }
    sb.append(action);

    if (sig.narration() != null && !isError) {
      sb.append(" — ").append(sig.narration());
    } else if (!params.isEmpty()) {
      sb.append(" for ").append(params);
    }

    if (node.children().isEmpty()) {
      renderOutcomeInline(node.outcome(), sig, sb);
      sb.append(".\n");
    } else {
      sb.append(":\n");
      for (var child : node.children()) {
        renderNode(child, depth + 1, sb);
      }
      renderOutcomeClosing(node.outcome(), indent, sb);
    }
  }

  private void renderOutcomeClosing(TraceOutcome outcome, String indent, StringBuilder sb) {
    if (outcome instanceof TraceOutcome.Returned r && r.renderedValue() != null) {
      sb.append(indent).append("  Returned ").append(r.renderedValue()).append(".\n");
    } else if (outcome instanceof TraceOutcome.Threw t) {
      sb.append(indent)
          .append("  ")
          .append(t.exception().getClass().getSimpleName())
          .append(": ")
          .append(t.exception().getMessage())
          .append(".\n");
    }
  }

  private void renderOutcomeInline(TraceOutcome outcome, MethodSignature sig, StringBuilder sb) {
    if (outcome instanceof TraceOutcome.Returned r && r.renderedValue() != null) {
      sb.append(", returning ").append(r.renderedValue());
    } else if (outcome instanceof TraceOutcome.Threw t) {
      sb.append(" — ")
          .append(t.exception().getClass().getSimpleName())
          .append(": ")
          .append(t.exception().getMessage());
      if (sig.errorContext() != null) {
        sb.append(" (").append(sig.errorContext()).append(")");
      }
    }
  }

  private String renderParams(TraceNode node) {
    var params = node.signature().parameters();
    if (params.isEmpty()) {
      return "";
    }
    return params.stream().map(this::renderParam).collect(Collectors.joining(" "));
  }

  private String renderParam(ParameterCapture param) {
    if (param.redacted()) {
      return param.name() + ": [REDACTED]";
    }
    return param.name() + ": " + param.renderedValue();
  }
}
