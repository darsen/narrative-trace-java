package ai.narrativetrace.core.render;

import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.TraceTree;

/** Builds YAML frontmatter blocks for Markdown trace output files. */
public final class FrontmatterBuilder {

  private String scenario;

  public FrontmatterBuilder scenario(String scenario) {
    this.scenario = scenario;
    return this;
  }

  public String build(TraceTree tree) {
    var sb = new StringBuilder("---\n");
    sb.append("type: trace\n");
    if (scenario != null) {
      sb.append("scenario: ").append(yamlSafe(scenario)).append("\n");
    }
    if (!tree.roots().isEmpty()) {
      var root = tree.roots().get(0);
      var sig = root.signature();
      sb.append("entry_point: ")
          .append(sig.className())
          .append(".")
          .append(sig.methodName())
          .append("\n");
      sb.append("duration_ms: ").append(root.durationMillis()).append("\n");
    }
    int methodCount = countNodes(tree);
    int errorCount = countErrors(tree);
    sb.append("method_count: ").append(methodCount).append("\n");
    sb.append("error_count: ").append(errorCount).append("\n");
    sb.append("---\n");
    return sb.toString();
  }

  private int countNodes(TraceTree tree) {
    int count = 0;
    for (var root : tree.roots()) {
      count += countNodes(root);
    }
    return count;
  }

  private int countNodes(TraceNode node) {
    int count = 1;
    for (var child : node.children()) {
      count += countNodes(child);
    }
    return count;
  }

  private int countErrors(TraceTree tree) {
    int count = 0;
    for (var root : tree.roots()) {
      count += countErrors(root);
    }
    return count;
  }

  private int countErrors(TraceNode node) {
    int count = (node.outcome() instanceof TraceOutcome.Threw) ? 1 : 0;
    for (var child : node.children()) {
      count += countErrors(child);
    }
    return count;
  }

  static String yamlSafe(String value) {
    if (value.indexOf(':') >= 0
        || value.indexOf('#') >= 0
        || value.indexOf('"') >= 0
        || value.indexOf('\\') >= 0
        || value.indexOf('\n') >= 0) {
      return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
    return value;
  }
}
