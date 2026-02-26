package ai.narrativetrace.slf4j;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jNarrativeContextTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logbackLogger;
    private Slf4jNarrativeContext context;

    @BeforeEach
    void setUp() {
        logbackLogger = (Logger) LoggerFactory.getLogger("narrativetrace");
        logbackLogger.setLevel(Level.ALL);
        logbackLogger.detachAndStopAllAppenders();
        appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);

        var delegate = new ThreadLocalNarrativeContext();
        delegate.reset();
        context = new Slf4jNarrativeContext(delegate);
    }

    @Test
    void emitsTraceLevelLogOnMethodEntry() {
        var signature = new MethodSignature("OrderService", "placeOrder", List.of(
                new ParameterCapture("orderId", "\"O-123\"", false)
        ));

        context.enterMethod(signature);

        assertThat(appender.list).hasSize(1);
        var event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.TRACE);
        assertThat(event.getFormattedMessage()).contains("OrderService.placeOrder");
    }

    @Test
    void emitsTraceLevelLogOnMethodReturn() {
        var signature = new MethodSignature("OrderService", "calculateTotal", List.of());
        context.enterMethod(signature);
        appender.list.clear();

        context.exitMethodWithReturn("99.0");

        assertThat(appender.list).hasSize(1);
        var event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.TRACE);
        assertThat(event.getFormattedMessage()).contains("returned");
        assertThat(event.getFormattedMessage()).contains("99.0");
    }

    @Test
    void emitsWarnLevelLogOnException() {
        var signature = new MethodSignature("PaymentService", "charge", List.of());
        context.enterMethod(signature);
        appender.list.clear();

        context.exitMethodWithException(new RuntimeException("Insufficient funds"), null);

        assertThat(appender.list).hasSize(1);
        var event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("RuntimeException");
        assertThat(event.getFormattedMessage()).contains("Insufficient funds");
    }

    @Test
    void loggerNameIsNarrativetrace() {
        var signature = new MethodSignature("Svc", "method", List.of());
        context.enterMethod(signature);

        assertThat(appender.list.get(0).getLoggerName()).isEqualTo("narrativetrace");
    }

    @Test
    void inclusMdcFieldsForClassMethodAndDepth() {
        var signature = new MethodSignature("OrderService", "placeOrder", List.of());

        context.enterMethod(signature);

        var mdc = appender.list.get(0).getMDCPropertyMap();
        assertThat(mdc).containsEntry("nt.class", "OrderService");
        assertThat(mdc).containsEntry("nt.method", "placeOrder");
        assertThat(mdc).containsKey("nt.depth");
    }

    @Test
    void customLevelMappingsOverrideDefaults() {
        var delegate = new ThreadLocalNarrativeContext();
        delegate.reset();
        var customContext = new Slf4jNarrativeContext(delegate, Map.of(
                Slf4jNarrativeContext.EventType.ENTRY, org.slf4j.event.Level.DEBUG,
                Slf4jNarrativeContext.EventType.RETURN, org.slf4j.event.Level.INFO,
                Slf4jNarrativeContext.EventType.EXCEPTION, org.slf4j.event.Level.ERROR
        ));

        var signature = new MethodSignature("Svc", "method", List.of());
        customContext.enterMethod(signature);

        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);

        appender.list.clear();
        customContext.exitMethodWithReturn("ok");
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);

        appender.list.clear();
        customContext.enterMethod(signature);
        appender.list.clear();
        customContext.exitMethodWithException(new RuntimeException("fail"), null);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void skipsLoggingWhenSlf4jLevelIsOff() {
        logbackLogger.setLevel(Level.OFF);
        var signature = new MethodSignature("Svc", "method", List.of());

        context.enterMethod(signature);

        assertThat(appender.list).isEmpty();
    }

    @Test
    void exitMethodWithReturnSkipsLogWhenLevelIsOff() {
        var signature = new MethodSignature("Svc", "method", List.of());
        context.enterMethod(signature);
        appender.list.clear();
        logbackLogger.setLevel(Level.OFF);

        context.exitMethodWithReturn("result");

        assertThat(appender.list).isEmpty();
    }

    @Test
    void captureTraceDelegatesToUnderlyingContext() {
        var signature = new MethodSignature("Svc", "method", List.of());
        context.enterMethod(signature);
        context.exitMethodWithReturn("ok");

        var tree = context.captureTrace();

        assertThat(tree.roots()).hasSize(1);
        assertThat(tree.roots().get(0).signature().methodName()).isEqualTo("method");
    }

    @Test
    void resetDelegatesToUnderlyingContext() {
        var signature = new MethodSignature("Svc", "method", List.of());
        context.enterMethod(signature);
        context.exitMethodWithReturn("ok");

        context.reset();

        assertThat(context.captureTrace().isEmpty()).isTrue();
    }

    @Test
    void enterMethodWithNoParametersFormatsEmptyParamString() {
        var signature = new MethodSignature("Svc", "method", List.of());
        context.enterMethod(signature);

        assertThat(appender.list.get(0).getFormattedMessage()).contains("Svc.method()");
    }

    @Test
    void enterMethodRedactsParameterInLog() {
        var signature = new MethodSignature("AuthService", "login", List.of(
                new ParameterCapture("username", "\"admin\"", false),
                new ParameterCapture("password", "\"s3cret\"", true)
        ));

        context.enterMethod(signature);

        var message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("[REDACTED]");
        assertThat(message).doesNotContain("s3cret");
    }

    @Test
    void enterMethodWithMultipleParametersJoinsWithComma() {
        var signature = new MethodSignature("Svc", "method", List.of(
                new ParameterCapture("a", "\"1\"", false),
                new ParameterCapture("b", "\"2\"", false)
        ));
        context.enterMethod(signature);

        assertThat(appender.list.get(0).getFormattedMessage()).contains("a: \"1\", b: \"2\"");
    }

    @Test
    void errorContextAppearsInExceptionLog() {
        var signature = new MethodSignature("PaymentService", "charge", List.of());
        context.enterMethod(signature);
        appender.list.clear();

        context.exitMethodWithException(new RuntimeException("declined"), "Payment declined for customer C-123");

        assertThat(appender.list).hasSize(1);
        var message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("RuntimeException");
        assertThat(message).contains("declined");
        assertThat(message).contains("Payment declined for customer C-123");
    }

    @Test
    void snapshotDelegatesToUnderlyingContext() {
        var snapshot = context.snapshot();

        assertThat(snapshot).isNotNull();
    }

    @Test
    void isActiveDelegatesToWrappedContext() {
        assertThat(context.isActive()).isTrue();

        var noopContext = new Slf4jNarrativeContext(
                ai.narrativetrace.core.context.NoopNarrativeContext.INSTANCE);
        assertThat(noopContext.isActive()).isFalse();
    }

    @Test
    void noErrorContextOmitsBracketsInExceptionLog() {
        var signature = new MethodSignature("PaymentService", "charge", List.of());
        context.enterMethod(signature);
        appender.list.clear();

        context.exitMethodWithException(new RuntimeException("declined"), null);

        var message = appender.list.get(0).getFormattedMessage();
        assertThat(message).doesNotContain("[");
        assertThat(message).doesNotContain("]");
    }
}
