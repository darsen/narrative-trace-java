package ai.narrativetrace.servlet;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.export.RequestContext;
import ai.narrativetrace.core.export.TraceExporter;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jTraceExporterTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logbackLogger;

    @BeforeEach
    void setUp() {
        logbackLogger = (Logger) LoggerFactory.getLogger("narrativetrace.export");
        logbackLogger.setLevel(Level.INFO);
        logbackLogger.detachAndStopAllAppenders();
        appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
    }

    @Test
    void implementsTraceExporter() {
        assertThat(new Slf4jTraceExporter()).isInstanceOf(TraceExporter.class);
    }

    @Test
    void logsTraceJsonAtInfo() {
        var tree = singleNodeTree();
        var requestContext = new RequestContext("GET", "/api/test", 200, 10L);

        new Slf4jTraceExporter().export(tree, requestContext);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void logIncludesTraceJson() {
        var tree = singleNodeTree();
        var requestContext = new RequestContext("GET", "/api/test", 200, 10L);

        new Slf4jTraceExporter().export(tree, requestContext);

        var message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("placeOrder");
        assertThat(message).contains("OrderService");
    }

    @Test
    void logIncludesRequestMetadata() {
        var tree = singleNodeTree();
        var requestContext = new RequestContext("POST", "/api/orders", 201, 55L);

        new Slf4jTraceExporter().export(tree, requestContext);

        var message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("POST");
        assertThat(message).contains("/api/orders");
        assertThat(message).contains("201");
        assertThat(message).contains("55ms");
    }

    @Test
    void skipsRenderingWhenInfoDisabled() {
        logbackLogger.setLevel(Level.WARN);
        var tree = singleNodeTree();
        var requestContext = new RequestContext("GET", "/api/test", 200, 10L);

        new Slf4jTraceExporter().export(tree, requestContext);

        assertThat(appender.list).isEmpty();
    }

    private DefaultTraceTree singleNodeTree() {
        var node = new TraceNode(
                new MethodSignature("OrderService", "placeOrder", List.of()),
                List.of(),
                new TraceOutcome.Returned("\"order-42\""),
                10_000_000L);
        return new DefaultTraceTree(List.of(node));
    }
}
