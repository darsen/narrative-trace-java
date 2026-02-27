/**
 * Trace context management and cross-thread propagation.
 *
 * <p>{@link ai.narrativetrace.core.context.NarrativeContext} is the central interface for recording
 * method entries, exits, and exceptions. {@link
 * ai.narrativetrace.core.context.ThreadLocalNarrativeContext} is the default implementation using a
 * ThreadLocal Deque-based call stack. {@link ai.narrativetrace.core.context.ContextSnapshot}
 * enables cross-thread trace propagation.
 *
 * <p>This package has zero external dependencies.
 */
package ai.narrativetrace.core.context;
