package ai.narrativetrace.benchmarks;

import ai.narrativetrace.core.annotation.Narrated;

public interface NarratedInterpolatedService {
  @Narrated("{input} processed")
  String execute(String input);
}
