package ai.narrativetrace.benchmarks;

import ai.narrativetrace.core.annotation.OnError;

public interface ErrorService {
  @OnError(exception = RuntimeException.class, value = "Failed for {input}")
  String execute(String input);
}
