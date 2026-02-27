package ai.narrativetrace.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.config.NarrativeTraceConfig;
import ai.narrativetrace.core.config.TracingLevel;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceOutcome;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ThreadLocalNarrativeContextTest {

  @Test
  void enterExitSingleMethodProducesOneRootNode() {
    var context = new ThreadLocalNarrativeContext();
    var signature =
        new MethodSignature(
            "OrderService",
            "placeOrder",
            List.of(new ParameterCapture("customerId", "\"C-123\"", false)));

    context.enterMethod(signature);
    context.exitMethodWithReturn("\"order-42\"");

    var tree = context.captureTrace();
    assertThat(tree.isEmpty()).isFalse();
    assertThat(tree.roots()).hasSize(1);

    var root = tree.roots().get(0);
    assertThat(root.signature().className()).isEqualTo("OrderService");
    assertThat(root.children()).isEmpty();
    assertThat(root.outcome()).isInstanceOf(TraceOutcome.Returned.class);
    assertThat(((TraceOutcome.Returned) root.outcome()).renderedValue()).isEqualTo("\"order-42\"");
  }

  @Test
  void nestedEnterExitProducesChildNodes() {
    var context = new ThreadLocalNarrativeContext();

    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    context.enterMethod(new MethodSignature("InventoryService", "checkStock", List.of()));
    context.exitMethodWithReturn("true");
    context.exitMethodWithReturn("\"order-42\"");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);

    var root = tree.roots().get(0);
    assertThat(root.signature().className()).isEqualTo("OrderService");
    assertThat(root.children()).hasSize(1);

    var child = root.children().get(0);
    assertThat(child.signature().className()).isEqualTo("InventoryService");
    assertThat(child.children()).isEmpty();
    assertThat(((TraceOutcome.Returned) child.outcome()).renderedValue()).isEqualTo("true");
  }

  @Test
  void exitWithExceptionCapturesThrew() {
    var context = new ThreadLocalNarrativeContext();
    var exception = new RuntimeException("insufficient funds");

    context.enterMethod(new MethodSignature("PaymentService", "charge", List.of()));
    context.exitMethodWithException(exception, null);

    var tree = context.captureTrace();
    var root = tree.roots().get(0);
    assertThat(root.outcome()).isInstanceOf(TraceOutcome.Threw.class);
    assertThat(((TraceOutcome.Threw) root.outcome()).exception()).isSameAs(exception);
  }

  @Test
  void capturesDurationOnMethodExit() {
    var context = new ThreadLocalNarrativeContext();
    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    // Simulate some work
    busyWait(5_000_000L); // at least 5ms
    context.exitMethodWithReturn("order-42");

    var tree = context.captureTrace();
    var root = tree.roots().get(0);
    assertThat(root.durationNanos()).isGreaterThan(0L);
  }

  @Test
  void nestedCallsHaveIndependentDurations() {
    var context = new ThreadLocalNarrativeContext();

    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    busyWait(1_000_000L);
    context.enterMethod(new MethodSignature("InventoryService", "checkStock", List.of()));
    busyWait(2_000_000L);
    context.exitMethodWithReturn("true");
    busyWait(1_000_000L);
    context.exitMethodWithReturn("\"order-42\"");

    var tree = context.captureTrace();
    var root = tree.roots().get(0);
    var child = root.children().get(0);

    assertThat(child.durationNanos()).isGreaterThan(0L);
    assertThat(root.durationNanos()).isGreaterThan(child.durationNanos());
  }

  @Test
  void exceptionExitCapturesDuration() {
    var context = new ThreadLocalNarrativeContext();
    context.enterMethod(new MethodSignature("PaymentService", "charge", List.of()));
    busyWait(2_000_000L);
    context.exitMethodWithException(new RuntimeException("declined"), null);

    var tree = context.captureTrace();
    var root = tree.roots().get(0);
    assertThat(root.outcome()).isInstanceOf(TraceOutcome.Threw.class);
    assertThat(root.durationNanos()).isGreaterThan(0L);
  }

  @Test
  void resetClearsTrace() {
    var context = new ThreadLocalNarrativeContext();

    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    context.exitMethodWithReturn("order-42");
    assertThat(context.captureTrace().isEmpty()).isFalse();

    context.reset();

    assertThat(context.captureTrace().isEmpty()).isTrue();
    assertThat(context.captureTrace().roots()).isEmpty();
  }

  @Test
  void detailLevelCapturesEverythingIncludingParameterValues() {
    var config = new NarrativeTraceConfig(TracingLevel.DETAIL);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(
        new MethodSignature(
            "OrderService",
            "placeOrder",
            List.of(
                new ParameterCapture("customerId", "\"C-123\"", false),
                new ParameterCapture("quantity", "5", false))));
    context.exitMethodWithReturn("\"order-42\"");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);

    var root = tree.roots().get(0);
    var params = root.signature().parameters();
    assertThat(params).hasSize(2);
    assertThat(params.get(0).renderedValue()).isEqualTo("\"C-123\"");
    assertThat(params.get(1).renderedValue()).isEqualTo("5");

    var outcome = (TraceOutcome.Returned) root.outcome();
    assertThat(outcome.renderedValue()).isEqualTo("\"order-42\"");
  }

  @Test
  void defaultConstructorUsesDetailLevel() {
    var context = new ThreadLocalNarrativeContext();

    context.enterMethod(
        new MethodSignature(
            "OrderService",
            "placeOrder",
            List.of(new ParameterCapture("customerId", "\"C-123\"", false))));
    context.exitMethodWithReturn("order-42");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);
    assertThat(tree.roots().get(0).signature().parameters().get(0).renderedValue())
        .isEqualTo("\"C-123\"");
  }

  @Test
  void narrativeLevelCapturesAllMethodsButSuppressesParameterValues() {
    var config = new NarrativeTraceConfig(TracingLevel.NARRATIVE);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(
        new MethodSignature(
            "OrderService",
            "placeOrder",
            List.of(
                new ParameterCapture("customerId", "\"C-123\"", false),
                new ParameterCapture("quantity", "5", false))));
    context.exitMethodWithReturn("order-42");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);

    var root = tree.roots().get(0);
    // Parameters are captured but values are suppressed
    var params = root.signature().parameters();
    assertThat(params).hasSize(2);
    assertThat(params.get(0).name()).isEqualTo("customerId");
    assertThat(params.get(0).renderedValue()).isEmpty();
    assertThat(params.get(1).name()).isEqualTo("quantity");
    assertThat(params.get(1).renderedValue()).isEmpty();
  }

  @Test
  void summaryLevelCapturesSingleCallAsRootAndLeaf() {
    var config = new NarrativeTraceConfig(TracingLevel.SUMMARY);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    context.exitMethodWithReturn("order-42");

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);
    assertThat(tree.roots().get(0).outcome()).isInstanceOf(TraceOutcome.Returned.class);
  }

  @Test
  void summaryLevelCapturesRootAndLeafDiscardsIntermediate() {
    var config = new NarrativeTraceConfig(TracingLevel.SUMMARY);
    var context = new ThreadLocalNarrativeContext(config);

    // root → intermediate → leaf
    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    context.enterMethod(new MethodSignature("ValidationService", "validate", List.of()));
    context.enterMethod(new MethodSignature("InventoryService", "checkStock", List.of()));
    context.exitMethodWithReturn("true"); // leaf — captured
    context.exitMethodWithReturn("valid"); // intermediate — discarded
    context.exitMethodWithReturn("order-42"); // root — captured

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);

    var root = tree.roots().get(0);
    assertThat(root.signature().className()).isEqualTo("OrderService");
    // intermediate was discarded, so root has one child (the leaf)
    assertThat(root.children()).hasSize(1);
    assertThat(root.children().get(0).signature().className()).isEqualTo("InventoryService");
  }

  @Test
  void summaryLevelCapturesExceptions() {
    var config = new NarrativeTraceConfig(TracingLevel.SUMMARY);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(new MethodSignature("PaymentService", "charge", List.of()));
    context.exitMethodWithException(new RuntimeException("declined"), null);

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);
    assertThat(tree.roots().get(0).outcome()).isInstanceOf(TraceOutcome.Threw.class);
  }

  @Test
  void errorsLevelCapturesOnlyExceptionPaths() {
    var config = new NarrativeTraceConfig(TracingLevel.ERRORS);
    var context = new ThreadLocalNarrativeContext(config);

    // Normal call — should not be captured
    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    context.exitMethodWithReturn("order-42");

    assertThat(context.captureTrace().isEmpty()).isTrue();

    // Exception call — should be captured
    context.reset();
    context.enterMethod(new MethodSignature("PaymentService", "charge", List.of()));
    context.exitMethodWithException(new RuntimeException("declined"), null);

    var tree = context.captureTrace();
    assertThat(tree.roots()).hasSize(1);
    assertThat(tree.roots().get(0).outcome()).isInstanceOf(TraceOutcome.Threw.class);
  }

  @Test
  void isActiveReturnsTrueAtDetailLevel() {
    var config = new NarrativeTraceConfig(TracingLevel.DETAIL);
    var context = new ThreadLocalNarrativeContext(config);
    assertThat(context.isActive()).isTrue();
  }

  @Test
  void isActiveReturnsFalseAtOffLevel() {
    var config = new NarrativeTraceConfig(TracingLevel.OFF);
    var context = new ThreadLocalNarrativeContext(config);
    assertThat(context.isActive()).isFalse();
  }

  @Test
  void skipsCaptureEntirelyWhenLevelIsOff() {
    var config = new NarrativeTraceConfig(TracingLevel.OFF);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
    context.exitMethodWithReturn("order-42");

    var tree = context.captureTrace();
    assertThat(tree.isEmpty()).isTrue();
  }

  @Test
  void offLevelSkipsExceptionCapture() {
    var config = new NarrativeTraceConfig(TracingLevel.OFF);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(new MethodSignature("Svc", "method", List.of()));
    context.exitMethodWithException(new RuntimeException("fail"), null);

    assertThat(context.captureTrace().isEmpty()).isTrue();
  }

  @Test
  void levelChangeFromOffToDetailDoesNotThrowOnExit() {
    var config = new NarrativeTraceConfig(TracingLevel.OFF);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(new MethodSignature("Svc", "method", List.of()));
    config.setLevel(TracingLevel.DETAIL);
    context.exitMethodWithReturn("ok");

    assertThat(context.captureTrace().isEmpty()).isTrue();
  }

  @Test
  void levelChangeFromOffToDetailDoesNotThrowOnExceptionExit() {
    var config = new NarrativeTraceConfig(TracingLevel.OFF);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(new MethodSignature("Svc", "method", List.of()));
    config.setLevel(TracingLevel.DETAIL);
    context.exitMethodWithException(new RuntimeException("fail"), null);

    assertThat(context.captureTrace().isEmpty()).isTrue();
  }

  @Test
  void levelChangeFromDetailToOffDoesNotThrow() {
    var config = new NarrativeTraceConfig(TracingLevel.DETAIL);
    var context = new ThreadLocalNarrativeContext(config);

    context.enterMethod(new MethodSignature("Svc", "method", List.of()));
    config.setLevel(TracingLevel.OFF);
    context.exitMethodWithReturn("ok");

    assertThat(context.captureTrace().isEmpty()).isTrue();
  }

  @Test
  void downgradeToOffDuringInFlightMethodDoesNotLeakFrame() {
    var config = new NarrativeTraceConfig(TracingLevel.DETAIL);
    var context = new ThreadLocalNarrativeContext(config);

    // Start a method while DETAIL is active — pushes a frame
    context.enterMethod(new MethodSignature("Svc", "first", List.of()));
    // Downgrade to OFF mid-flight
    config.setLevel(TracingLevel.OFF);
    context.exitMethodWithReturn("ok");

    // Restore DETAIL and trace a second method
    config.setLevel(TracingLevel.DETAIL);
    context.enterMethod(new MethodSignature("Svc", "second", List.of()));
    context.exitMethodWithReturn("ok");

    var tree = context.captureTrace();
    // second call should be its own root, not a child of a leaked frame
    assertThat(tree.roots()).anyMatch(r -> r.signature().methodName().equals("second"));
    var secondRoots =
        tree.roots().stream().filter(r -> r.signature().methodName().equals("second")).toList();
    assertThat(secondRoots).hasSize(1);
    assertThat(secondRoots.get(0).children()).isEmpty();
  }

  @Test
  void downgradeToOffDuringInFlightExceptionDoesNotLeakFrame() {
    var config = new NarrativeTraceConfig(TracingLevel.DETAIL);
    var context = new ThreadLocalNarrativeContext(config);

    // Start a method while DETAIL is active — pushes a frame
    context.enterMethod(new MethodSignature("Svc", "first", List.of()));
    // Downgrade to OFF mid-flight
    config.setLevel(TracingLevel.OFF);
    context.exitMethodWithException(new RuntimeException("fail"), null);

    // Restore DETAIL and trace a second method
    config.setLevel(TracingLevel.DETAIL);
    context.enterMethod(new MethodSignature("Svc", "second", List.of()));
    context.exitMethodWithReturn("ok");

    var tree = context.captureTrace();
    var secondRoots =
        tree.roots().stream().filter(r -> r.signature().methodName().equals("second")).toList();
    assertThat(secondRoots).hasSize(1);
    assertThat(secondRoots.get(0).children()).isEmpty();
  }

  @Test
  void errorContextFlowsToTraceNodeSignatureOnException() {
    var context = new ThreadLocalNarrativeContext();
    context.enterMethod(new MethodSignature("PaymentService", "charge", List.of()));
    context.exitMethodWithException(
        new RuntimeException("declined"), "Payment was declined for customer C-123");

    var tree = context.captureTrace();
    var root = tree.roots().get(0);
    assertThat(root.signature().errorContext())
        .isEqualTo("Payment was declined for customer C-123");
  }

  @Test
  void childThreadGetsFreshContextViaSnapshot() throws Exception {
    var context = new ThreadLocalNarrativeContext();

    // Trace on parent thread
    context.enterMethod(new MethodSignature("ParentService", "parentMethod", List.of()));
    context.exitMethodWithReturn("parent-result");

    var snapshot = context.snapshot();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      var childTree =
          executor
              .submit(
                  () -> {
                    try (var scope = snapshot.activate()) {
                      context.enterMethod(
                          new MethodSignature("ChildService", "childMethod", List.of()));
                      context.exitMethodWithReturn("child-result");
                      return context.captureTrace();
                    }
                  })
              .get();

      // Child thread captures its own trace independently
      assertThat(childTree.roots()).hasSize(1);
      assertThat(childTree.roots().get(0).signature().className()).isEqualTo("ChildService");

      // Parent thread's trace is unaffected
      var parentTree = context.captureTrace();
      assertThat(parentTree.roots()).hasSize(1);
      assertThat(parentTree.roots().get(0).signature().className()).isEqualTo("ParentService");
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void scopeRestoresPreviousStateOnPooledThread() throws Exception {
    var context = new ThreadLocalNarrativeContext();
    var snapshot = context.snapshot();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      // Pre-populate the pooled thread with existing traces
      executor
          .submit(
              () -> {
                context.enterMethod(
                    new MethodSignature("ExistingService", "existingMethod", List.of()));
                context.exitMethodWithReturn("existing");
              })
          .get();

      // Now activate snapshot on the same pooled thread — should get fresh stack
      executor
          .submit(
              () -> {
                try (var scope = snapshot.activate()) {
                  context.enterMethod(new MethodSignature("NewService", "newMethod", List.of()));
                  context.exitMethodWithReturn("new");
                  var duringScope = context.captureTrace();
                  assertThat(duringScope.roots()).hasSize(1);
                  assertThat(duringScope.roots().get(0).signature().className())
                      .isEqualTo("NewService");
                }

                // After scope closes, the previous state should be restored
                var afterScope = context.captureTrace();
                assertThat(afterScope.roots()).hasSize(1);
                assertThat(afterScope.roots().get(0).signature().className())
                    .isEqualTo("ExistingService");
              })
          .get();
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void wrapRunnableActivatesScopeAndCloses() throws Exception {
    var context = new ThreadLocalNarrativeContext();
    var snapshot = context.snapshot();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      var wrappedTree =
          executor
              .submit(
                  () -> {
                    var wrapped =
                        snapshot.wrap(
                            (Runnable)
                                () -> {
                                  context.enterMethod(
                                      new MethodSignature("WrappedService", "run", List.of()));
                                  context.exitMethodWithReturn(null);
                                });
                    wrapped.run();
                    return context.captureTrace();
                  })
              .get();

      // After the wrapped runnable completes, scope is closed — child thread has empty tree
      assertThat(wrappedTree.isEmpty()).isTrue();
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void wrapCallableActivatesScopeAndReturnsResult() throws Exception {
    var context = new ThreadLocalNarrativeContext();
    var snapshot = context.snapshot();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Callable<ai.narrativetrace.core.tree.TraceTree> callable =
          () -> {
            context.enterMethod(new MethodSignature("CalcService", "compute", List.of()));
            context.exitMethodWithReturn("42");
            return context.captureTrace();
          };
      var result = executor.submit(snapshot.wrap(callable)).get();

      assertThat(result.roots()).hasSize(1);
      assertThat(result.roots().get(0).signature().className()).isEqualTo("CalcService");
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void wrapSupplierWorksWithCompletableFuture() throws Exception {
    var context = new ThreadLocalNarrativeContext();
    var snapshot = context.snapshot();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Supplier<ai.narrativetrace.core.tree.TraceTree> supplier =
          () -> {
            context.enterMethod(new MethodSignature("AsyncService", "fetch", List.of()));
            context.exitMethodWithReturn("data");
            return context.captureTrace();
          };
      var result = CompletableFuture.supplyAsync(snapshot.wrap(supplier), executor).get();

      assertThat(result.roots()).hasSize(1);
      assertThat(result.roots().get(0).signature().className()).isEqualTo("AsyncService");
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void snapshotReturnsNonNullAndActivateReturnsScope() {
    var context = new ThreadLocalNarrativeContext();

    var snapshot = context.snapshot();
    assertThat(snapshot).isNotNull();

    var scope = snapshot.activate();
    assertThat(scope).isNotNull();
    scope.close(); // should not throw
  }

  private void busyWait(long nanos) {
    long start = System.nanoTime();
    while (System.nanoTime() - start < nanos) {
      Thread.onSpinWait();
    }
  }
}
