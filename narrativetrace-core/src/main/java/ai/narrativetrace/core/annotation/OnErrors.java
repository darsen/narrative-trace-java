package ai.narrativetrace.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeatable {@link OnError} annotations.
 *
 * <p>This annotation is automatically applied by the compiler when multiple {@code @OnError}
 * annotations are placed on the same method. It should not normally be used directly.
 *
 * @see OnError
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnErrors {
  /**
   * The contained {@link OnError} annotations.
   *
   * @return the array of error templates
   */
  OnError[] value();
}
