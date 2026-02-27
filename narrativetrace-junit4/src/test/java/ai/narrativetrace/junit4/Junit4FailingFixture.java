package ai.narrativetrace.junit4;

import static org.junit.Assert.fail;

import ai.narrativetrace.core.event.MethodSignature;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class Junit4FailingFixture {

  @Rule public NarrativeTraceRule narrativeTrace = new NarrativeTraceRule();

  @Test
  public void failingTest() {
    narrativeTrace.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
    narrativeTrace.context().exitMethodWithReturn("ok");
    fail("deliberate failure");
  }
}
