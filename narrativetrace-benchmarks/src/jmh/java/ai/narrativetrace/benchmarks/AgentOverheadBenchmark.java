package ai.narrativetrace.benchmarks;

import ai.narrativetrace.agent.AgentRuntime;
import ai.narrativetrace.benchmarks.agent.AgentTargetService;
import ai.narrativetrace.core.config.NarrativeTraceConfig;
import ai.narrativetrace.core.config.TracingLevel;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class AgentOverheadBenchmark {

    private AgentTargetService service;
    private NarrativeContext detailContext;
    private NarrativeContext offContext;

    @Setup(Level.Trial)
    public void setup() {
        detailContext = new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.DETAIL));
        offContext = new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.OFF));
        AgentRuntime.setContext(detailContext);
        service = new AgentTargetService();
    }

    @Setup(Level.Invocation)
    public void resetContext() {
        detailContext.reset();
        offContext.reset();
    }

    @Benchmark
    public void agent_noAnnotations(Blackhole bh) {
        AgentRuntime.setContext(detailContext);
        bh.consume(service.execute("test"));
    }

    @Benchmark
    public void agent_OFF(Blackhole bh) {
        AgentRuntime.setContext(offContext);
        bh.consume(service.execute("test"));
    }
}
