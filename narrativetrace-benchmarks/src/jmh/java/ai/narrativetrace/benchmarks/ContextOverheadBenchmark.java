package ai.narrativetrace.benchmarks;

import ai.narrativetrace.core.config.NarrativeTraceConfig;
import ai.narrativetrace.core.config.TracingLevel;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.NoopNarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ContextOverheadBenchmark {

  private static final MethodSignature SIGNATURE =
      new MethodSignature("TestService", "execute", List.of(), null, null);

  private NarrativeContext detailContext;
  private NarrativeContext narrativeContext;
  private NarrativeContext summaryContext;
  private NarrativeContext errorsContext;
  private NarrativeContext offContext;

  @Setup(Level.Trial)
  public void setup() {
    detailContext = new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.DETAIL));
    narrativeContext =
        new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.NARRATIVE));
    summaryContext =
        new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.SUMMARY));
    errorsContext = new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.ERRORS));
    offContext = new ThreadLocalNarrativeContext(new NarrativeTraceConfig(TracingLevel.OFF));
  }

  @Setup(Level.Invocation)
  public void resetContexts() {
    detailContext.reset();
    narrativeContext.reset();
    summaryContext.reset();
    errorsContext.reset();
    offContext.reset();
  }

  @Benchmark
  public void context_enterExit_DETAIL(Blackhole bh) {
    detailContext.enterMethod(SIGNATURE);
    detailContext.exitMethodWithReturn("result");
    bh.consume(detailContext.captureTrace());
  }

  @Benchmark
  public void context_enterExit_NARRATIVE(Blackhole bh) {
    narrativeContext.enterMethod(SIGNATURE);
    narrativeContext.exitMethodWithReturn("result");
    bh.consume(narrativeContext.captureTrace());
  }

  @Benchmark
  public void context_enterExit_SUMMARY(Blackhole bh) {
    summaryContext.enterMethod(SIGNATURE);
    summaryContext.exitMethodWithReturn("result");
    bh.consume(summaryContext.captureTrace());
  }

  @Benchmark
  public void context_enterExit_ERRORS(Blackhole bh) {
    errorsContext.enterMethod(SIGNATURE);
    errorsContext.exitMethodWithReturn("result");
    bh.consume(errorsContext.captureTrace());
  }

  @Benchmark
  public void context_enterExit_OFF(Blackhole bh) {
    offContext.enterMethod(SIGNATURE);
    offContext.exitMethodWithReturn("result");
    bh.consume(offContext.captureTrace());
  }

  @Benchmark
  public void context_enterExit_NOOP(Blackhole bh) {
    NoopNarrativeContext.INSTANCE.enterMethod(SIGNATURE);
    NoopNarrativeContext.INSTANCE.exitMethodWithReturn("result");
    bh.consume(NoopNarrativeContext.INSTANCE.captureTrace());
  }
}
