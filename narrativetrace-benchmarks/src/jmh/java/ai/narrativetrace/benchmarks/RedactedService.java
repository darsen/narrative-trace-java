package ai.narrativetrace.benchmarks;

import ai.narrativetrace.core.annotation.NotTraced;

public interface RedactedService {
  String execute(@NotTraced String input);
}
