package ai.narrativetrace.micrometer;

import ai.narrativetrace.core.context.ContextSnapshot;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class NarrativeTraceThreadLocalAccessorTest {

    @Test
    void hasKeyAndCanRegister() {
        var accessor = new NarrativeTraceThreadLocalAccessor();
        assertThat(accessor.key()).isEqualTo("narrativetrace");

        var registry = new ContextRegistry();
        registry.registerThreadLocalAccessor(accessor);
    }

    @Test
    void getValueReturnsSnapshot() {
        var context = new ThreadLocalNarrativeContext();
        var accessor = new NarrativeTraceThreadLocalAccessor(context);

        ContextSnapshot snapshot = accessor.getValue();
        assertThat(snapshot).isNotNull();
    }

    @Test
    void setValueActivatesSnapshotAndResetRestores() {
        var context = new ThreadLocalNarrativeContext();
        var accessor = new NarrativeTraceThreadLocalAccessor(context);

        // Trace something on this thread
        context.enterMethod(new MethodSignature("Original", "method", List.of()));
        context.exitMethodWithReturn("orig");
        assertThat(context.captureTrace().roots()).hasSize(1);

        // setValue activates a fresh scope
        var snapshot = accessor.getValue();
        accessor.setValue(snapshot);

        // Fresh scope — empty trace
        context.enterMethod(new MethodSignature("Scoped", "work", List.of()));
        context.exitMethodWithReturn("scoped");
        assertThat(context.captureTrace().roots()).hasSize(1);
        assertThat(context.captureTrace().roots().get(0).signature().className()).isEqualTo("Scoped");

        // setValue() (no-arg) restores previous state
        accessor.setValue();

        var restored = context.captureTrace();
        assertThat(restored.roots()).hasSize(1);
        assertThat(restored.roots().get(0).signature().className()).isEqualTo("Original");
    }

    @Test
    void endToEndWithContextSnapshotFactory() throws Exception {
        var context = new ThreadLocalNarrativeContext();
        var accessor = new NarrativeTraceThreadLocalAccessor(context);

        var registry = new ContextRegistry();
        registry.registerThreadLocalAccessor(accessor);

        var factory = ContextSnapshotFactory.builder()
                .contextRegistry(registry)
                .build();

        // Capture snapshot on main thread
        var micrometerSnapshot = factory.captureAll();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var childTree = executor.submit(() -> {
                try (var scope = micrometerSnapshot.setThreadLocals()) {
                    context.enterMethod(new MethodSignature("ChildService", "process", List.of()));
                    context.exitMethodWithReturn("done");
                    return context.captureTrace();
                }
            }).get();

            assertThat(childTree.roots()).hasSize(1);
            assertThat(childTree.roots().get(0).signature().className()).isEqualTo("ChildService");
        } finally {
            executor.shutdown();
        }
    }
}
