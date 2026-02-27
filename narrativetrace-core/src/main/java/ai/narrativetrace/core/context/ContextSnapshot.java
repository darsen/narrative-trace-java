package ai.narrativetrace.core.context;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Enables cross-thread trace propagation by capturing and restoring context state.
 *
 * <p>Obtain a snapshot via {@link NarrativeContext#snapshot()}, then pass it to another thread and
 * {@linkplain #activate() activate} it. The {@link ContextScope} returned by {@code activate()}
 * restores the previous context when closed.
 *
 * <p>Convenience methods {@link #wrap(Runnable)}, {@link #wrap(Callable)}, and {@link
 * #wrap(Supplier)} simplify usage with executors and CompletableFuture:
 *
 * <pre>{@code
 * var snapshot = context.snapshot();
 * executor.submit(snapshot.wrap(() -> {
 *     // trace events here are captured in the parent context
 *     service.processOrder(orderId);
 * }));
 * }</pre>
 *
 * @see NarrativeContext#snapshot()
 * @see ContextScope
 */
public interface ContextSnapshot {

  /**
   * Activates this snapshot on the current thread. The returned scope must be closed (typically via
   * try-with-resources) to restore the previous context.
   *
   * @return a closeable scope that restores the previous context on close
   */
  ContextScope activate();

  /**
   * Wraps a {@link Runnable} to activate this snapshot before execution.
   *
   * @param task the task to wrap
   * @return a wrapped task that activates and deactivates the snapshot
   */
  default Runnable wrap(Runnable task) {
    return () -> {
      try (var scope = activate()) {
        task.run();
      }
    };
  }

  /**
   * Wraps a {@link Callable} to activate this snapshot before execution.
   *
   * @param task the task to wrap
   * @param <T> the return type
   * @return a wrapped task that activates and deactivates the snapshot
   */
  default <T> Callable<T> wrap(Callable<T> task) {
    return () -> {
      try (var scope = activate()) {
        return task.call();
      }
    };
  }

  /**
   * Wraps a {@link Supplier} to activate this snapshot before execution.
   *
   * @param task the task to wrap
   * @param <T> the return type
   * @return a wrapped task that activates and deactivates the snapshot
   */
  default <T> Supplier<T> wrap(Supplier<T> task) {
    return () -> {
      try (var scope = activate()) {
        return task.get();
      }
    };
  }
}
