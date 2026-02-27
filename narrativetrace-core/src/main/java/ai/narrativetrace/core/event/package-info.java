/**
 * Trace event data models: method signatures, parameters, outcomes, and trace nodes.
 *
 * <p>All types in this package are records or sealed interfaces. {@link
 * ai.narrativetrace.core.event.TraceNode} represents a single method invocation with its
 * parameters, outcome, children, and timing. {@link ai.narrativetrace.core.event.ParameterCapture}
 * holds pre-rendered parameter values. {@link ai.narrativetrace.core.event.TraceOutcome} is a
 * sealed interface with {@code Returned} and {@code Threw} variants.
 */
package ai.narrativetrace.core.event;
