package ai.narrativetrace.core.tree;

import ai.narrativetrace.core.event.TraceNode;
import java.util.List;

/** Standard immutable {@link TraceTree} implementation backed by a list of root nodes. */
public final class DefaultTraceTree implements TraceTree {

  private final List<TraceNode> roots;

  public DefaultTraceTree(List<TraceNode> roots) {
    this.roots = List.copyOf(roots);
  }

  @Override
  public List<TraceNode> roots() {
    return roots;
  }

  @Override
  public boolean isEmpty() {
    return roots.isEmpty();
  }
}
