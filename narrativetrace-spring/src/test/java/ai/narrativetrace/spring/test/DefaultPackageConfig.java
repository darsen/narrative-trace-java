package ai.narrativetrace.spring.test;

import ai.narrativetrace.spring.EnableNarrativeTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableNarrativeTrace
public class DefaultPackageConfig {
  @Bean
  GreetingService greetingService() {
    return new DefaultGreetingService();
  }
}
