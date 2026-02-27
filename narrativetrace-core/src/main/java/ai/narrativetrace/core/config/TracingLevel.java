package ai.narrativetrace.core.config;

/**
 * Controls the verbosity of trace capture, from no capture to full parameter detail.
 *
 * <p>Levels are ordered by increasing verbosity. A higher level enables all behaviors of the levels
 * below it:
 *
 * <table>
 * <caption>Tracing levels</caption>
 * <tr><th>Level</th><th>Captures</th><th>Performance</th></tr>
 * <tr><td>{@link #OFF}</td><td>Nothing</td><td>Zero overhead</td></tr>
 * <tr><td>{@link #ERRORS}</td><td>Exception paths only</td><td>Minimal</td></tr>
 * <tr><td>{@link #SUMMARY}</td><td>Root + leaf calls</td><td>Low</td></tr>
 * <tr><td>{@link #NARRATIVE}</td><td>All calls, no param values</td><td>Moderate</td></tr>
 * <tr><td>{@link #DETAIL}</td><td>All calls + full param values</td><td>Full</td></tr>
 * </table>
 *
 * <p>The level can be changed at runtime via {@link NarrativeTraceConfig#setLevel(TracingLevel)}
 * using a volatile field for immediate visibility across threads.
 *
 * @see NarrativeTraceConfig
 */
public enum TracingLevel {

  /** No trace capture. {@code NarrativeContext.isActive()} returns {@code false}. */
  OFF,

  /** Only exception paths are recorded. Successful calls are discarded. */
  ERRORS,

  /** Root and leaf calls only. Intermediate call frames are pruned. */
  SUMMARY,

  /** All calls recorded, but parameter values are suppressed. */
  NARRATIVE,

  /** All calls recorded with full parameter values. Default for tests. */
  DETAIL;

  /**
   * Returns whether this level is at least as verbose as the required level.
   *
   * @param required the minimum level to check against
   * @return {@code true} if this level enables the required level's behavior
   */
  public boolean isEnabled(TracingLevel required) {
    return this.ordinal() >= required.ordinal();
  }
}
