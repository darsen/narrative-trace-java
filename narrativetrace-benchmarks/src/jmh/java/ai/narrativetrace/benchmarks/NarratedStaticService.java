package ai.narrativetrace.benchmarks;

import ai.narrativetrace.core.annotation.Narrated;

public interface NarratedStaticService {
  @Narrated("Processing request")
  String execute(String input);
}
