package ai.narrativetrace.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Enables automatic NarrativeTrace instrumentation for Spring-managed beans.
 *
 * <p>Apply to a {@code @Configuration} class to activate the {@link
 * NarrativeTraceBeanPostProcessor}, which wraps eligible beans in tracing proxies.
 *
 * <pre>{@code
 * @Configuration
 * @EnableNarrativeTrace(basePackages = "com.example.service")
 * public class AppConfig { }
 * }</pre>
 *
 * <p>The bean post-processor runs at {@code HIGHEST_PRECEDENCE} so the tracing proxy is the
 * innermost wrapper. With {@code @EnableAsync}, the async proxy wraps outside, ensuring trace
 * capture happens on the async thread.
 *
 * <p>If no {@code basePackages} are specified, all beans implementing at least one interface are
 * eligible for tracing.
 *
 * @see NarrativeTraceConfiguration
 * @see NarrativeTraceBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({NarrativeTraceRegistrar.class, NarrativeTraceConfiguration.class})
public @interface EnableNarrativeTrace {
  /**
   * Package prefixes to limit which beans are wrapped in tracing proxies. An empty array (default)
   * means all interface-implementing beans are eligible.
   *
   * @return package prefixes to trace
   */
  String[] basePackages() default {};
}
