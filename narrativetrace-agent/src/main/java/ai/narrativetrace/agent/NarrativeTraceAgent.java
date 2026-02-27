package ai.narrativetrace.agent;

import java.lang.instrument.Instrumentation;

/** Java agent entry point ({@code premain}) that registers the class file transformer. */
public final class NarrativeTraceAgent {

  private NarrativeTraceAgent() {}

  public static void premain(String agentArgs, Instrumentation inst) {
    var config = AgentConfig.parse(agentArgs);
    if (inst != null) {
      inst.addTransformer(new NarrativeClassFileTransformer(config));
    }
  }
}
