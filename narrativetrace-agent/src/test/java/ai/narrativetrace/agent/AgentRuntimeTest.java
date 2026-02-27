package ai.narrativetrace.agent;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.config.NarrativeTraceConfig;
import ai.narrativetrace.core.config.TracingLevel;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentRuntimeTest {

  private ThreadLocalNarrativeContext context;

  @BeforeEach
  void setUp() {
    context = new ThreadLocalNarrativeContext();
    context.reset();
    AgentRuntime.setContext(context);
  }

  @Test
  void setAndGetContext() {
    assertThat(AgentRuntime.getContext()).isSameAs(context);
  }

  @Test
  void enterMethodWithoutParameters() {
    AgentRuntime.enterMethod("MyClass", "myMethod");
    AgentRuntime.exitMethodWithReturn("result");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);
    assertThat(tree.roots().get(0).signature().className()).isEqualTo("MyClass");
    assertThat(tree.roots().get(0).signature().methodName()).isEqualTo("myMethod");
    assertThat(tree.roots().get(0).signature().parameters()).isEmpty();
  }

  @Test
  void skipsEnterMethodCaptureWhenContextIsNotActive() {
    var offConfig = new NarrativeTraceConfig(TracingLevel.OFF);
    var offContext = new ThreadLocalNarrativeContext(offConfig);
    offContext.reset();
    AgentRuntime.setContext(offContext);

    AgentRuntime.enterMethod(
        "MyClass",
        "myMethod",
        new String[] {"x"},
        new Object[] {"val"},
        new boolean[] {false},
        null);
    AgentRuntime.exitMethodWithReturn("result");

    assertThat(offContext.captureTrace().roots()).isEmpty();
  }

  @Test
  void skipsExitMethodWithReturnRenderingWhenContextIsNotActive() {
    var offConfig = new NarrativeTraceConfig(TracingLevel.OFF);
    var offContext = new ThreadLocalNarrativeContext(offConfig);
    offContext.reset();
    AgentRuntime.setContext(offContext);

    AgentRuntime.exitMethodWithReturn(new Object());

    assertThat(offContext.captureTrace().roots()).isEmpty();
  }

  @Test
  void resolveErrorContextReturnsNullForNonMatchingException() {
    var result =
        AgentRuntime.resolveErrorContext(
            new RuntimeException("boom"),
            new String[] {"Error for {x}"},
            new String[] {"Ljava/lang/IllegalArgumentException;"},
            new String[] {"x"},
            new Object[] {"val"},
            new boolean[] {false});

    assertThat(result).isNull();
  }

  @Test
  void resolveErrorContextReturnsNullForInvalidDescriptor() {
    var result =
        AgentRuntime.resolveErrorContext(
            new RuntimeException("boom"),
            new String[] {"Error"},
            new String[] {"InvalidDescriptor"},
            new String[] {"x"},
            new Object[] {"val"},
            new boolean[] {false});

    assertThat(result).isNull();
  }

  @Test
  void resolveErrorContextReturnsNullForNonexistentExceptionClass() {
    var result =
        AgentRuntime.resolveErrorContext(
            new RuntimeException("boom"),
            new String[] {"Error for {x}"},
            new String[] {"Lcom/nonexistent/FakeException;"},
            new String[] {"x"},
            new Object[] {"val"},
            new boolean[] {false});

    assertThat(result).isNull();
  }
}
