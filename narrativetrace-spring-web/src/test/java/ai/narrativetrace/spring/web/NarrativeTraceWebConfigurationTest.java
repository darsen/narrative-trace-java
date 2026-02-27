package ai.narrativetrace.spring.web;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.export.TraceExporter;
import ai.narrativetrace.servlet.NarrativeTraceFilter;
import ai.narrativetrace.spring.EnableNarrativeTrace;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class NarrativeTraceWebConfigurationTest {

  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setUp() {
    var logbackLogger = (Logger) LoggerFactory.getLogger("narrativetrace.export");
    logbackLogger.setLevel(Level.INFO);
    logbackLogger.detachAndStopAllAppenders();
    appender = new ListAppender<>();
    appender.start();
    logbackLogger.addAppender(appender);
  }

  @Configuration
  @EnableNarrativeTrace
  static class TestConfig {}

  @Test
  void configProvidesFilterBean() {
    try (var ctx =
        new AnnotationConfigApplicationContext(
            TestConfig.class, NarrativeTraceWebConfiguration.class)) {
      assertThat(ctx.getBean(NarrativeTraceFilter.class)).isNotNull();
    }
  }

  @Test
  void defaultExporterIsSlf4j() throws Exception {
    try (var ctx =
        new AnnotationConfigApplicationContext(
            TestConfig.class, NarrativeTraceWebConfiguration.class)) {
      var filter = ctx.getBean(NarrativeTraceFilter.class);
      var narrativeContext = ctx.getBean(NarrativeContext.class);

      narrativeContext.reset();
      filter.doFilter(
          new MockHttpServletRequest("GET", "/api/test"),
          new MockHttpServletResponse(),
          (req, res) -> {
            narrativeContext.enterMethod(new MethodSignature("Svc", "handle", List.of()));
            narrativeContext.exitMethodWithReturn("\"ok\"");
          });

      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
    }
  }

  @Configuration
  @EnableNarrativeTrace
  static class CustomExporterConfig {
    static final List<String> EXPORTED = new ArrayList<>();

    @Bean
    public TraceExporter customExporter() {
      return (tree, reqCtx) -> EXPORTED.add(reqCtx.uri());
    }
  }

  @Test
  void customExporterReplacesDefault() throws Exception {
    CustomExporterConfig.EXPORTED.clear();
    try (var ctx =
        new AnnotationConfigApplicationContext(
            CustomExporterConfig.class, NarrativeTraceWebConfiguration.class)) {
      var filter = ctx.getBean(NarrativeTraceFilter.class);
      var narrativeContext = ctx.getBean(NarrativeContext.class);

      narrativeContext.reset();
      filter.doFilter(
          new MockHttpServletRequest("GET", "/custom"),
          new MockHttpServletResponse(),
          (req, res) -> {
            narrativeContext.enterMethod(new MethodSignature("Svc", "handle", List.of()));
            narrativeContext.exitMethodWithReturn("\"ok\"");
          });

      assertThat(CustomExporterConfig.EXPORTED).containsExactly("/custom");
      assertThat(appender.list).isEmpty();
    }
  }

  @Test
  void fullLifecycleIntegration() throws Exception {
    try (var ctx =
        new AnnotationConfigApplicationContext(
            TestConfig.class, NarrativeTraceWebConfiguration.class)) {
      var filter = ctx.getBean(NarrativeTraceFilter.class);
      var narrativeContext = ctx.getBean(NarrativeContext.class);

      narrativeContext.reset();
      var request = new MockHttpServletRequest("POST", "/api/orders");
      var response = new MockHttpServletResponse();
      response.setStatus(201);

      filter.doFilter(
          request,
          response,
          (req, res) -> {
            narrativeContext.enterMethod(
                new MethodSignature("OrderService", "placeOrder", List.of()));
            narrativeContext.exitMethodWithReturn("\"order-42\"");
          });

      assertThat(narrativeContext.captureTrace().isEmpty()).isTrue();
      assertThat(appender.list).hasSize(1);
      var message = appender.list.get(0).getFormattedMessage();
      assertThat(message).contains("POST");
      assertThat(message).contains("/api/orders");
      assertThat(message).contains("201");
      assertThat(message).contains("placeOrder");
    }
  }
}
