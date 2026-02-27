package ai.narrativetrace.spring.web;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.export.TraceExporter;
import ai.narrativetrace.servlet.NarrativeTraceFilter;
import ai.narrativetrace.servlet.Slf4jTraceExporter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring configuration that registers the NarrativeTraceFilter with pluggable TraceExporter. */
@Configuration
public class NarrativeTraceWebConfiguration {

  @Bean
  public NarrativeTraceFilter narrativeTraceFilter(
      NarrativeContext context, ObjectProvider<TraceExporter> exporterProvider) {
    var exporter = exporterProvider.getIfAvailable(Slf4jTraceExporter::new);
    return new NarrativeTraceFilter(context, exporter);
  }
}
