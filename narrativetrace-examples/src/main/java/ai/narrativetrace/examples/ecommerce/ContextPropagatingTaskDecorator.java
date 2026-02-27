package ai.narrativetrace.examples.ecommerce;

import io.micrometer.context.ContextSnapshotFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class ContextPropagatingTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    var snapshot = ContextSnapshotFactory.builder().build().captureAll();
    var mdc = MDC.getCopyOfContextMap();
    return () -> {
      var previousMdc = MDC.getCopyOfContextMap();
      if (mdc != null) {
        MDC.setContextMap(mdc);
      }
      try {
        snapshot.wrap(runnable).run();
      } finally {
        if (previousMdc != null) {
          MDC.setContextMap(previousMdc);
        } else {
          MDC.clear();
        }
      }
    };
  }
}
