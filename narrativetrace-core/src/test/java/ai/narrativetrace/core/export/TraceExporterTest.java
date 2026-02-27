package ai.narrativetrace.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class TraceExporterTest {

  @Test
  void lambdaAssignmentAndCall() {
    var called = new AtomicBoolean(false);
    TraceExporter exporter = (tree, requestContext) -> called.set(true);

    exporter.export(new DefaultTraceTree(List.of()), new RequestContext("GET", "/", 200, 0L));

    assertThat(called).isTrue();
  }
}
