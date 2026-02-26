package ai.narrativetrace.spring;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NarrativeTraceConfiguration {

    @Bean
    public static NarrativeContext narrativeContext() {
        return new ThreadLocalNarrativeContext();
    }
}
