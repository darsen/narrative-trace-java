package ai.narrativetrace.core.config;

/**
 * Runtime configuration for trace capture behavior.
 *
 * <p>Holds the active {@link TracingLevel} as a volatile field, allowing runtime level changes that
 * take effect immediately across all threads sharing this config instance.
 *
 * <pre>{@code
 * var config = new NarrativeTraceConfig(TracingLevel.NARRATIVE);
 * var context = new ThreadLocalNarrativeContext(config);
 *
 * // Change level at runtime (thread-safe)
 * config.setLevel(TracingLevel.DETAIL);
 * }</pre>
 *
 * @see TracingLevel
 * @see ai.narrativetrace.core.context.ThreadLocalNarrativeContext
 */
public final class NarrativeTraceConfig {

  private volatile TracingLevel level;

  /** Creates a config with default level ({@link TracingLevel#DETAIL}). */
  public NarrativeTraceConfig() {
    this(TracingLevel.DETAIL);
  }

  /**
   * Creates a config with the specified tracing level.
   *
   * @param level the initial tracing level
   */
  public NarrativeTraceConfig(TracingLevel level) {
    this.level = level;
  }

  /**
   * Returns the current tracing level.
   *
   * @return the active level (volatile read)
   */
  public TracingLevel level() {
    return level;
  }

  /**
   * Changes the tracing level at runtime.
   *
   * <p>The change is immediately visible to all threads (volatile write).
   *
   * @param level the new tracing level
   */
  public void setLevel(TracingLevel level) {
    this.level = level;
  }
}
