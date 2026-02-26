package ai.narrativetrace.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(OnErrors.class)
public @interface OnError {
    String value();
    Class<? extends Throwable> exception() default Throwable.class;
}
