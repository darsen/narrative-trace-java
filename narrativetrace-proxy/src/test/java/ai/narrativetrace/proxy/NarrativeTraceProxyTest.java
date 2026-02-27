package ai.narrativetrace.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.narrativetrace.core.annotation.Narrated;
import ai.narrativetrace.core.annotation.NotTraced;
import ai.narrativetrace.core.annotation.OnError;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.NoopNarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.TraceTree;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NarrativeTraceProxyTest {

  interface OrderService {
    String placeOrder(String customerId);
  }

  @Test
  void delegatesToRealImplementationAndReturnsValue() {
    OrderService real = customerId -> "order-42";
    var context = new ThreadLocalNarrativeContext();

    OrderService proxy = NarrativeTraceProxy.trace(real, OrderService.class, context);
    var result = proxy.placeOrder("C-123");

    assertThat(result).isEqualTo("order-42");
  }

  @Test
  void capturesMethodEntryAndReturnInContext() {
    OrderService real = customerId -> "order-42";
    var context = new ThreadLocalNarrativeContext();

    OrderService proxy = NarrativeTraceProxy.trace(real, OrderService.class, context);
    proxy.placeOrder("C-123");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);

    var root = tree.roots().get(0);
    assertThat(root.signature().className()).isEqualTo("OrderService");
    assertThat(root.signature().methodName()).isEqualTo("placeOrder");
    assertThat(root.signature().parameters()).hasSize(1);
    assertThat(root.signature().parameters().get(0).name()).isEqualTo("customerId");
    assertThat(root.signature().parameters().get(0).renderedValue()).isEqualTo("\"C-123\"");
    assertThat(root.outcome()).isInstanceOf(TraceOutcome.Returned.class);
    assertThat(((TraceOutcome.Returned) root.outcome()).renderedValue()).isEqualTo("\"order-42\"");
  }

  interface InventoryService {
    boolean checkStock(String itemId);
  }

  @Test
  void twoProxiedServicesShareContextForNestedTrace() {
    var context = new ThreadLocalNarrativeContext();

    InventoryService inventoryProxy =
        NarrativeTraceProxy.trace(
            (InventoryService) itemId -> true, InventoryService.class, context);
    OrderService orderProxy =
        NarrativeTraceProxy.trace(
            (OrderService)
                customerId -> {
                  inventoryProxy.checkStock("ITEM-1");
                  return "order-42";
                },
            OrderService.class,
            context);

    orderProxy.placeOrder("C-123");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);

    var root = tree.roots().get(0);
    assertThat(root.signature().className()).isEqualTo("OrderService");
    assertThat(root.children()).hasSize(1);

    var child = root.children().get(0);
    assertThat(child.signature().className()).isEqualTo("InventoryService");
    assertThat(child.signature().methodName()).isEqualTo("checkStock");
  }

  interface PaymentService {
    void charge(double amount);
  }

  @Test
  void capturesExceptionAndRethrows() {
    var exception = new IllegalStateException("insufficient funds");
    PaymentService real =
        amount -> {
          throw exception;
        };
    var context = new ThreadLocalNarrativeContext();

    PaymentService proxy = NarrativeTraceProxy.trace(real, PaymentService.class, context);

    assertThatThrownBy(() -> proxy.charge(99.95)).isSameAs(exception);

    var tree = context.captureTrace();
    var root = tree.roots().get(0);
    assertThat(root.outcome()).isInstanceOf(TraceOutcome.Threw.class);
    assertThat(((TraceOutcome.Threw) root.outcome()).exception()).isSameAs(exception);
  }

  interface NarratedOrderService {
    @Narrated("Placing order of {quantity} units for customer {customerId}")
    String placeOrder(String customerId, int quantity);
  }

  @Test
  void detectsNarratedAnnotationAndResolvesTemplate() {
    NarratedOrderService real = (customerId, quantity) -> "order-42";
    var context = new ThreadLocalNarrativeContext();

    var proxy = NarrativeTraceProxy.trace(real, NarratedOrderService.class, context);
    proxy.placeOrder("C-123", 5);

    var tree = context.captureTrace();
    var sig = tree.roots().get(0).signature();
    assertThat(sig.narration()).isEqualTo("Placing order of 5 units for customer C-123");
  }

  interface OnErrorService {
    @OnError("Context: charging customer {customerId}, amount was {amount}")
    void charge(String customerId, double amount);
  }

  @Test
  void detectsOnErrorAnnotationAndResolvesTemplate() {
    OnErrorService real =
        (customerId, amount) -> {
          throw new RuntimeException("declined");
        };
    var context = new ThreadLocalNarrativeContext();

    var proxy = NarrativeTraceProxy.trace(real, OnErrorService.class, context);
    assertThatThrownBy(() -> proxy.charge("C-123", 99.95)).isInstanceOf(RuntimeException.class);

    var tree = context.captureTrace();
    var sig = tree.roots().get(0).signature();
    assertThat(sig.errorContext()).isEqualTo("Context: charging customer C-123, amount was 99.95");
  }

  interface AuthService {
    boolean login(String username, @NotTraced String password);
  }

  @Test
  void respectsNotTracedAnnotationAsRedacted() {
    AuthService real = (username, password) -> true;
    var context = new ThreadLocalNarrativeContext();

    AuthService proxy = NarrativeTraceProxy.trace(real, AuthService.class, context);
    proxy.login("admin", "secret");

    var tree = context.captureTrace();
    var params = tree.roots().get(0).signature().parameters();
    assertThat(params.get(0).redacted()).isFalse();
    assertThat(params.get(1).redacted()).isTrue();
    assertThat(params.get(1).name()).isEqualTo("password");
  }

  interface SpecificExceptionService {
    @OnError(value = "Payment declined for {customerId}", exception = IllegalStateException.class)
    void charge(String customerId);
  }

  @Test
  void matchesSpecificExceptionType() {
    SpecificExceptionService real =
        customerId -> {
          throw new IllegalStateException("declined");
        };
    var context = new ThreadLocalNarrativeContext();

    var proxy = NarrativeTraceProxy.trace(real, SpecificExceptionService.class, context);
    assertThatThrownBy(() -> proxy.charge("C-123")).isInstanceOf(IllegalStateException.class);

    var tree = context.captureTrace();
    var sig = tree.roots().get(0).signature();
    assertThat(sig.errorContext()).isEqualTo("Payment declined for C-123");
  }

  interface MostSpecificWinsService {
    @OnError("General error")
    @OnError(value = "Specific: state error", exception = IllegalStateException.class)
    void process();
  }

  @Test
  void mostSpecificExceptionTypeWinsOverDefault() {
    MostSpecificWinsService real =
        () -> {
          throw new IllegalStateException("bad state");
        };
    var context = new ThreadLocalNarrativeContext();

    var proxy = NarrativeTraceProxy.trace(real, MostSpecificWinsService.class, context);
    assertThatThrownBy(proxy::process).isInstanceOf(IllegalStateException.class);

    var tree = context.captureTrace();
    var sig = tree.roots().get(0).signature();
    assertThat(sig.errorContext()).isEqualTo("Specific: state error");
  }

  interface NonMatchingService {
    @OnError(value = "IO problem", exception = java.io.IOException.class)
    void process();
  }

  @Test
  void nonMatchingAnnotationProducesNoErrorContext() {
    NonMatchingService real =
        () -> {
          throw new IllegalArgumentException("wrong arg");
        };
    var context = new ThreadLocalNarrativeContext();

    var proxy = NarrativeTraceProxy.trace(real, NonMatchingService.class, context);
    assertThatThrownBy(proxy::process).isInstanceOf(IllegalArgumentException.class);

    var tree = context.captureTrace();
    var sig = tree.roots().get(0).signature();
    assertThat(sig.errorContext()).isNull();
  }

  interface StatusService {
    String status();
  }

  @Test
  void enterAndExitCountsAreAlwaysBalanced() {
    var enterCount = new AtomicInteger();
    var exitCount = new AtomicInteger();
    var context =
        new NarrativeContext() {
          @Override
          public void enterMethod(MethodSignature sig) {
            enterCount.incrementAndGet();
          }

          @Override
          public void exitMethodWithReturn(String val) {
            exitCount.incrementAndGet();
          }

          @Override
          public void exitMethodWithException(Throwable ex, String ctx) {
            exitCount.incrementAndGet();
          }

          @Override
          public TraceTree captureTrace() {
            return null;
          }

          @Override
          public void reset() {}

          @Override
          public ai.narrativetrace.core.context.ContextSnapshot snapshot() {
            return null;
          }
        };

    PaymentService proxy =
        NarrativeTraceProxy.trace(
            (PaymentService)
                amount -> {
                  throw new IllegalStateException("fail");
                },
            PaymentService.class,
            context);
    try {
      proxy.charge(99.0);
    } catch (Exception ignored) {
    }

    assertThat(enterCount.get()).isEqualTo(1);
    assertThat(exitCount.get()).isEqualTo(1);
  }

  @Test
  void noArgMethodPassesNullArgsFromJdkProxy() {
    StatusService real = () -> "ok";
    var context = new ThreadLocalNarrativeContext();

    var proxy = NarrativeTraceProxy.trace(real, StatusService.class, context);
    var result = proxy.status();

    assertThat(result).isEqualTo("ok");
    var tree = context.captureTrace();
    assertThat(tree.roots().get(0).signature().parameters()).isEmpty();
  }

  interface RedactedNarrationService {
    @Narrated("Authenticating {username} with password {password}")
    boolean login(String username, @NotTraced String password);
  }

  @Test
  void redactedParametersMaskedInNarration() {
    RedactedNarrationService real = (username, password) -> true;
    var context = new ThreadLocalNarrativeContext();

    var proxy = NarrativeTraceProxy.trace(real, RedactedNarrationService.class, context);
    proxy.login("admin", "s3cret");

    var tree = context.captureTrace();
    var sig = tree.roots().get(0).signature();
    assertThat(sig.narration()).isEqualTo("Authenticating admin with password [REDACTED]");
  }

  interface Greeter {
    String greet(String name);
  }

  interface Auditable {
    String audit(String action);
  }

  static class GreeterAuditor implements Greeter, Auditable {
    @Override
    public String greet(String name) {
      return "Hello, " + name;
    }

    @Override
    public String audit(String action) {
      return "audited: " + action;
    }
  }

  @Test
  void multiInterfaceProxyCapturesExceptionAndRethrows() {
    var context = new ThreadLocalNarrativeContext();
    var target =
        new GreeterAuditor() {
          @Override
          public String greet(String name) {
            throw new RuntimeException("boom");
          }
        };

    var proxy =
        NarrativeTraceProxy.trace(target, new Class<?>[] {Greeter.class, Auditable.class}, context);

    assertThatThrownBy(() -> ((Greeter) proxy).greet("Alice"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("boom");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);
    assertThat(tree.roots().get(0).outcome()).isInstanceOf(TraceOutcome.Threw.class);
  }

  @Test
  void skipsAllCaptureWhenContextIsNotActive() {
    var enterCount = new AtomicInteger();
    var context =
        new NarrativeContext() {
          @Override
          public boolean isActive() {
            return false;
          }

          @Override
          public void enterMethod(MethodSignature sig) {
            enterCount.incrementAndGet();
          }

          @Override
          public void exitMethodWithReturn(String val) {
            enterCount.incrementAndGet();
          }

          @Override
          public void exitMethodWithException(Throwable ex, String ctx) {
            enterCount.incrementAndGet();
          }

          @Override
          public TraceTree captureTrace() {
            return null;
          }

          @Override
          public void reset() {}

          @Override
          public ai.narrativetrace.core.context.ContextSnapshot snapshot() {
            return null;
          }
        };

    OrderService proxy =
        NarrativeTraceProxy.trace(
            (OrderService) customerId -> "order-42", OrderService.class, context);
    var result = proxy.placeOrder("C-123");

    assertThat(result).isEqualTo("order-42");
    assertThat(enterCount.get()).isZero();
  }

  @Test
  void skipsAllCaptureAndRethrowsWhenContextIsNotActive() {
    var context = NoopNarrativeContext.INSTANCE;
    PaymentService proxy =
        NarrativeTraceProxy.trace(
            (PaymentService)
                amount -> {
                  throw new IllegalStateException("declined");
                },
            PaymentService.class,
            context);

    assertThatThrownBy(() -> proxy.charge(99.0))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("declined");
    assertThat(context.captureTrace().roots()).isEmpty();
  }

  @Test
  void skipsAllCaptureOnMultiInterfaceProxyWhenContextIsNotActive() {
    var context = NoopNarrativeContext.INSTANCE;
    var target = new GreeterAuditor();

    var proxy =
        NarrativeTraceProxy.trace(target, new Class<?>[] {Greeter.class, Auditable.class}, context);
    var result = ((Greeter) proxy).greet("Alice");

    assertThat(result).isEqualTo("Hello, Alice");
    assertThat(context.captureTrace().roots()).isEmpty();
  }

  @Test
  void skipsAllCaptureOnMultiInterfaceProxyAndRethrowsWhenNotActive() {
    var context = NoopNarrativeContext.INSTANCE;
    var target =
        new GreeterAuditor() {
          @Override
          public String greet(String name) {
            throw new RuntimeException("boom");
          }
        };
    var proxy =
        NarrativeTraceProxy.trace(target, new Class<?>[] {Greeter.class, Auditable.class}, context);

    assertThatThrownBy(() -> ((Greeter) proxy).greet("Alice"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("boom");
    assertThat(context.captureTrace().roots()).isEmpty();
  }

  @Test
  void emptyInterfacesArrayThrowsMeaningfulError() {
    var context = new ThreadLocalNarrativeContext();
    var target = new Object();

    assertThatThrownBy(() -> NarrativeTraceProxy.trace(target, new Class<?>[0], context))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("interface");
  }

  @Test
  void tracesCallsAcrossMultipleInterfaces() {
    var context = new ThreadLocalNarrativeContext();
    var target = new GreeterAuditor();

    var proxy =
        NarrativeTraceProxy.trace(target, new Class<?>[] {Greeter.class, Auditable.class}, context);

    var greetResult = ((Greeter) proxy).greet("Alice");
    var auditResult = ((Auditable) proxy).audit("login");

    assertThat(greetResult).isEqualTo("Hello, Alice");
    assertThat(auditResult).isEqualTo("audited: login");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(2);
    assertThat(tree.roots().get(0).signature().className()).isEqualTo("Greeter");
    assertThat(tree.roots().get(1).signature().className()).isEqualTo("Auditable");
  }
}
