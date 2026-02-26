package ai.narrativetrace.benchmarks;

import ai.narrativetrace.core.config.NarrativeTraceConfig;
import ai.narrativetrace.core.config.TracingLevel;
import ai.narrativetrace.core.context.NoopNarrativeContext;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.proxy.NarrativeTraceProxy;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ProxyOverheadBenchmark {

    private PlainService directPlain;
    private PlainService proxyPlain;
    private NarratedStaticService proxyNarratedStatic;
    private NarratedInterpolatedService proxyNarratedInterpolated;
    private RedactedService proxyRedacted;
    private SummaryService proxySummary;
    private ErrorService proxyError;
    private PlainService proxyNoop;
    private PlainService proxyOff;

    private NarrativeContext detailContext;
    private NarrativeContext offContext;

    @Setup(Level.Trial)
    public void setup() {
        detailContext = new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.DETAIL));
        offContext = new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.OFF));

        PlainService plainImpl = input -> "result:" + input;

        directPlain = plainImpl;
        proxyPlain = NarrativeTraceProxy.trace(plainImpl, PlainService.class, detailContext);

        proxyNarratedStatic = NarrativeTraceProxy.trace(
                (NarratedStaticService) input -> "result:" + input,
                NarratedStaticService.class, detailContext);

        proxyNarratedInterpolated = NarrativeTraceProxy.trace(
                (NarratedInterpolatedService) input -> "result:" + input,
                NarratedInterpolatedService.class, detailContext);

        proxyRedacted = NarrativeTraceProxy.trace(
                (RedactedService) input -> "result:" + input,
                RedactedService.class, detailContext);

        proxySummary = NarrativeTraceProxy.trace(
                (SummaryService) input -> new SummaryResult(input),
                SummaryService.class, detailContext);

        proxyError = NarrativeTraceProxy.trace(
                (ErrorService) input -> "result:" + input,
                ErrorService.class, detailContext);

        proxyNoop = NarrativeTraceProxy.trace(plainImpl, PlainService.class,
                NoopNarrativeContext.INSTANCE);

        proxyOff = NarrativeTraceProxy.trace(plainImpl, PlainService.class, offContext);
    }

    @Setup(Level.Invocation)
    public void resetContexts() {
        detailContext.reset();
        offContext.reset();
    }

    @Benchmark
    public void directCall(Blackhole bh) {
        bh.consume(directPlain.execute("test"));
    }

    @Benchmark
    public void proxy_noAnnotations(Blackhole bh) {
        bh.consume(proxyPlain.execute("test"));
    }

    @Benchmark
    public void proxy_narrated_static(Blackhole bh) {
        bh.consume(proxyNarratedStatic.execute("test"));
    }

    @Benchmark
    public void proxy_narrated_interpolated(Blackhole bh) {
        bh.consume(proxyNarratedInterpolated.execute("test"));
    }

    @Benchmark
    public void proxy_notTraced(Blackhole bh) {
        bh.consume(proxyRedacted.execute("test"));
    }

    @Benchmark
    public void proxy_narrativeSummary(Blackhole bh) {
        bh.consume(proxySummary.execute("test"));
    }

    @Benchmark
    public void proxy_onError(Blackhole bh) {
        bh.consume(proxyError.execute("test"));
    }

    @Benchmark
    public void proxy_noopContext(Blackhole bh) {
        bh.consume(proxyNoop.execute("test"));
    }

    @Benchmark
    public void proxy_OFF(Blackhole bh) {
        bh.consume(proxyOff.execute("test"));
    }
}
