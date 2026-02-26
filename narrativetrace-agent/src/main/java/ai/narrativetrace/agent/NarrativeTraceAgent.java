package ai.narrativetrace.agent;

import java.lang.instrument.Instrumentation;

public final class NarrativeTraceAgent {

    private NarrativeTraceAgent() {}

    public static void premain(String agentArgs, Instrumentation inst) {
        var config = AgentConfig.parse(agentArgs);
        if (inst != null) {
            inst.addTransformer(new NarrativeClassFileTransformer(config));
        }
    }
}
