package ai.narrativetrace.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides an exception-specific narrative template for a traced method.
 *
 * <p>When the method throws an exception matching {@link #exception()}, the template in {@link
 * #value()} replaces the default error description. Supports the same {@code {paramName}}
 * placeholder syntax as {@link Narrated}.
 *
 * <pre>{@code
 * @OnError(value = "order {orderId} was rejected: insufficient stock",
 *          exception = InsufficientStockException.class)
 * @OnError(value = "order {orderId} failed: payment declined",
 *          exception = PaymentDeclinedException.class)
 * OrderResult placeOrder(String orderId, int quantity);
 * }</pre>
 *
 * <p>Exception matching uses {@code isAssignableFrom} — a handler for {@code RuntimeException}
 * catches all its subclasses. When multiple {@code @OnError} annotations match, the most specific
 * exception type wins.
 *
 * <p>This annotation is {@link java.lang.annotation.Repeatable @Repeatable}; multiple instances are
 * collected into {@link OnErrors}.
 *
 * @see Narrated
 * @see OnErrors
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(OnErrors.class)
public @interface OnError {
  /**
   * The narrative template for this error case, with {@code {paramName}} placeholders.
   *
   * @return the error template string
   */
  String value();

  /**
   * The exception type this template applies to. Defaults to {@link Throwable}, which matches all
   * exceptions.
   *
   * @return the exception class to match
   */
  Class<? extends Throwable> exception() default Throwable.class;
}
