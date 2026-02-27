package ai.narrativetrace.core.event;

/**
 * A captured method parameter with its pre-rendered value.
 *
 * <p>Values are eagerly serialized at capture time via {@code ValueRenderer}. The {@code
 * renderedValue} is a String that includes quotes for string values (e.g., {@code "\"order-42\""})
 * and plain text for numbers/booleans (e.g., {@code "42"}). Empty string indicates a suppressed
 * value (non-DETAIL tracing level).
 *
 * @param name the parameter name (from {@code -parameters} compiler flag)
 * @param renderedValue the pre-rendered string representation
 * @param redacted {@code true} if marked with {@code @NotTraced}
 */
public record ParameterCapture(String name, String renderedValue, boolean redacted) {}
