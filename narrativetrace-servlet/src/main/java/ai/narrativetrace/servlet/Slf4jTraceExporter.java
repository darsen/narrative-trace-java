package ai.narrativetrace.servlet;

import ai.narrativetrace.core.export.JsonExporter;
import ai.narrativetrace.core.export.RequestContext;
import ai.narrativetrace.core.export.TraceExporter;
import ai.narrativetrace.core.tree.TraceTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports captured traces as JSON via SLF4J at INFO level.
 *
 * <p>Logs to the {@code narrativetrace.export} logger category with the format: {@code GET
 * /api/orders [200] 42ms — {json}}.
 *
 * <p>Skips export when INFO level is disabled for the logger.
 *
 * @see NarrativeTraceFilter
 * @see ai.narrativetrace.core.export.TraceExporter
 */
public class Slf4jTraceExporter implements TraceExporter {

  private static final Logger LOGGER = LoggerFactory.getLogger("narrativetrace.export");
  private final JsonExporter jsonExporter = new JsonExporter();

  @Override
  public void export(TraceTree tree, RequestContext requestContext) {
    if (!LOGGER.isInfoEnabled()) {
      return;
    }
    var json = jsonExporter.export(tree);
    LOGGER.info(
        "{} {} [{}] {}ms — {}",
        requestContext.method(),
        requestContext.uri(),
        requestContext.statusCode(),
        requestContext.durationMillis(),
        json);
  }
}
