package ai.narrativetrace.junit4;

import ai.narrativetrace.core.event.MethodSignature;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class Junit4SecondMultiTestFixture {

  @ClassRule public static NarrativeTraceClassRule classRule = new NarrativeTraceClassRule();

  @Rule public NarrativeTraceRule narrativeTrace = classRule.testRule();

  @Test
  public void customerChecksInventory() {
    narrativeTrace
        .context()
        .enterMethod(new MethodSignature("InventoryService", "checkStock", List.of()));
    narrativeTrace.context().exitMethodWithReturn("in-stock");
  }
}
