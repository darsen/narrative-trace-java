package ai.narrativetrace.core.render;

import ai.narrativetrace.core.tree.TraceTree;

/**
 * Functional interface for transforming a trace tree into a string representation.
 *
 * <p>Built-in implementations: {@link MarkdownRenderer}, {@link ProseRenderer}, {@link
 * IndentedTextRenderer}. Diagram renderers in {@code narrativetrace-diagrams} use method references
 * to adapt to this interface.
 *
 * <pre>{@code
 * NarrativeRenderer renderer = new MarkdownRenderer();
 * String output = renderer.render(traceTree);
 * }</pre>
 *
 * @see MarkdownRenderer
 * @see ProseRenderer
 * @see IndentedTextRenderer
 */
public interface NarrativeRenderer {

  /**
   * Renders the trace tree to a string.
   *
   * @param tree the trace tree to render
   * @return the rendered output
   */
  String render(TraceTree tree);
}
