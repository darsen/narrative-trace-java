package ai.narrativetrace.core.export;

import ai.narrativetrace.core.tree.TraceTree;

/**
 * Functional interface for exporting captured traces at request boundaries.
 *
 * <p>Implementations receive the trace tree and request metadata (HTTP method, URI, status code,
 * duration) after each request completes.
 *
 * <p>Built-in implementation: {@code ai.narrativetrace.servlet.Slf4jTraceExporter} (logs JSON via
 * SLF4J).
 *
 * @see RequestContext
 */
@FunctionalInterface
public interface TraceExporter {

  /**
   * Exports a captured trace with its request context.
   *
   * @param tree the captured trace tree (never empty when called by the filter)
   * @param requestContext metadata about the HTTP request
   */
  void export(TraceTree tree, RequestContext requestContext);
}
