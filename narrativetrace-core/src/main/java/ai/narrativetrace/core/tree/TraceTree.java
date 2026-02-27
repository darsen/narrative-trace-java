package ai.narrativetrace.core.tree;

import ai.narrativetrace.core.event.TraceNode;
import java.util.List;

/**
 * Immutable representation of a completed trace — the top-level result of trace capture.
 *
 * <p>A trace tree contains zero or more root {@link TraceNode}s, each representing a top-level
 * method invocation and its nested call tree. Trees are created by {@link
 * ai.narrativetrace.core.context.NarrativeContext#captureTrace()}.
 *
 * @see TraceNode
 * @see ai.narrativetrace.core.context.NarrativeContext
 */
public interface TraceTree {

  /**
   * Returns the root-level trace nodes.
   *
   * @return immutable list of root nodes (empty if no methods were traced)
   */
  List<TraceNode> roots();

  /**
   * Returns whether this tree contains any traced method calls.
   *
   * @return {@code true} if there are no root nodes
   */
  boolean isEmpty();
}
