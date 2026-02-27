package ai.narrativetrace.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter as redacted in all trace output channels.
 *
 * <p>When applied to a method parameter, the parameter value is replaced with {@code "***"} in
 * Markdown, prose, diagram, and JSON output. The parameter name still appears in the trace — only
 * its value is suppressed.
 *
 * <pre>{@code
 * void authenticate(String username, @NotTraced String password);
 * // Trace output: "authenticate username="admin" password=***"
 * }</pre>
 *
 * <p>Use this for sensitive data (passwords, tokens, PII) that should never appear in trace files.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface NotTraced {}
