package ai.narrativetrace.benchmarks.services;

import ai.narrativetrace.benchmarks.NarratedInterpolatedService;

public class NarratedInterpolatedServiceImpl implements NarratedInterpolatedService {
  @Override
  public String execute(String input) {
    return "result:" + input;
  }
}
