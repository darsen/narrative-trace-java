/**
 * JDK dynamic proxy for automatic trace capture on interface methods.
 *
 * <p>{@link ai.narrativetrace.proxy.NarrativeTraceProxy} wraps any interface implementation in a
 * tracing proxy that records method entries, parameters, return values, and exceptions. Works with
 * any interface without bytecode manipulation. Requires the {@code -parameters} compiler flag for
 * meaningful parameter names.
 */
package ai.narrativetrace.proxy;
