package ai.narrativetrace.servlet;

import ai.narrativetrace.core.export.JsonExporter;
import ai.narrativetrace.core.export.RequestContext;
import ai.narrativetrace.core.export.TraceExporter;
import ai.narrativetrace.core.tree.TraceTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jTraceExporter implements TraceExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("narrativetrace.export");
    private final JsonExporter jsonExporter = new JsonExporter();

    @Override
    public void export(TraceTree tree, RequestContext requestContext) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        var json = jsonExporter.export(tree);
        LOGGER.info("{} {} [{}] {}ms — {}", requestContext.method(), requestContext.uri(),
                requestContext.statusCode(), requestContext.durationMillis(), json);
    }
}
