package ai.narrativetrace.core.context;

/**
 * Scope handle returned by {@link ContextSnapshot#activate()} that restores the previous context on
 * close. Use with try-with-resources.
 *
 * @see ContextSnapshot
 */
public interface ContextScope extends AutoCloseable {
  /** Restores the previous context. Does not throw checked exceptions. */
  @Override
  void close();
}
