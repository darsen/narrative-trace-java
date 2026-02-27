package ai.narrativetrace.core.event;

/**
 * Sealed interface representing how a traced method completed.
 *
 * <p>Two variants:
 *
 * <ul>
 *   <li>{@link Returned} — normal completion with a pre-rendered return value
 *   <li>{@link Threw} — exceptional completion with the thrown exception
 * </ul>
 */
public sealed interface TraceOutcome {

  /**
   * Normal method completion.
   *
   * @param renderedValue the return value pre-rendered to String, or {@code null} for void
   */
  record Returned(String renderedValue) implements TraceOutcome {}

  /**
   * Exceptional method completion.
   *
   * @param exception the thrown exception
   */
  record Threw(Throwable exception) implements TraceOutcome {}
}
