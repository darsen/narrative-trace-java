package ai.narrativetrace.spring;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default Spring configuration that provides a {@link NarrativeContext} bean.
 *
 * <p>Imported automatically by {@link EnableNarrativeTrace @EnableNarrativeTrace}. Registers a
 * {@link ThreadLocalNarrativeContext} as a static {@code @Bean} to avoid circular dependency issues
 * with the bean post-processor.
 *
 * <p>Override the {@code narrativeContext} bean to use a different implementation (e.g., {@code
 * ai.narrativetrace.slf4j.Slf4jNarrativeContext}):
 *
 * <pre>{@code
 * @Bean
 * public static NarrativeContext narrativeContext() {
 *     return new Slf4jNarrativeContext(new ThreadLocalNarrativeContext());
 * }
 * }</pre>
 *
 * @see EnableNarrativeTrace
 */
@Configuration
public class NarrativeTraceConfiguration {

  /**
   * Creates the default {@link ThreadLocalNarrativeContext} bean.
   *
   * @return a new ThreadLocal-based narrative context
   */
  @Bean
  public static NarrativeContext narrativeContext() {
    return new ThreadLocalNarrativeContext();
  }
}
