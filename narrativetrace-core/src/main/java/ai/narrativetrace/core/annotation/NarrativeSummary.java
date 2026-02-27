package ai.narrativetrace.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-arg method as the custom summary renderer for its declaring type.
 *
 * <p>When {@code ValueRenderer} encounters an object whose class has a method annotated with
 * {@code @NarrativeSummary}, it calls that method instead of using {@code toString()} or reflective
 * POJO introspection.
 *
 * <pre>{@code
 * public class Order {
 *     private String id;
 *     private BigDecimal total;
 *
 *     @NarrativeSummary
 *     public String narrativeSummary() {
 *         return "Order " + id + " ($" + total + ")";
 *     }
 * }
 * }</pre>
 *
 * <p>The annotated method must be public, accept no arguments, and return {@code String}.
 *
 * @see ai.narrativetrace.core.render.ValueRenderer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NarrativeSummary {}
