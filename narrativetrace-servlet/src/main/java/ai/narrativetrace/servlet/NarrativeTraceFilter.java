package ai.narrativetrace.servlet;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.export.RequestContext;
import ai.narrativetrace.core.export.TraceExporter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet filter for production request lifecycle trace capture.
 *
 * <p>Follows a reset-chain-capture-export-reset lifecycle on each HTTP request:
 *
 * <ol>
 *   <li>Reset the narrative context (clear any stale state)
 *   <li>Execute the filter chain (downstream code is traced via proxies/agent)
 *   <li>Capture the accumulated trace tree
 *   <li>Export via the configured {@link TraceExporter}
 *   <li>Reset again (clean up)
 * </ol>
 *
 * <p>This class has zero Spring dependencies — it works with any servlet container. For Spring
 * integration, use {@code ai.narrativetrace.spring.web.NarrativeTraceWebConfiguration}.
 *
 * @see TraceExporter
 * @see Slf4jTraceExporter
 */
public class NarrativeTraceFilter implements Filter {

  private final NarrativeContext context;
  private final TraceExporter exporter;

  /**
   * Creates a filter with the given context and exporter.
   *
   * @param context the narrative context shared with tracing proxies/agent
   * @param exporter the exporter to send captured traces to
   */
  public NarrativeTraceFilter(NarrativeContext context, TraceExporter exporter) {
    this.context = context;
    this.exporter = exporter;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!(request instanceof HttpServletRequest httpRequest)
        || !(response instanceof HttpServletResponse httpResponse)) {
      chain.doFilter(request, response);
      return;
    }
    context.reset();
    long startTime = System.currentTimeMillis();
    try {
      chain.doFilter(request, response);
    } finally {
      var tree = context.captureTrace();
      try {
        if (!tree.isEmpty()) {
          long duration = System.currentTimeMillis() - startTime;
          var requestContext =
              new RequestContext(
                  httpRequest.getMethod(),
                  httpRequest.getRequestURI(),
                  httpResponse.getStatus(),
                  duration);
          try {
            exporter.export(tree, requestContext);
          } catch (Exception e) { // NOPMD
            // Observability failure must never become a request failure
          }
        }
      } finally {
        context.reset();
      }
    }
  }
}
