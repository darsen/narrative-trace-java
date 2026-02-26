package ai.narrativetrace.core.context;

import ai.narrativetrace.core.event.MethodSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoopNarrativeContextTest {

    @Test
    void allMethodsNoOpAndTraceIsEmpty() {
        var context = NoopNarrativeContext.INSTANCE;

        context.enterMethod(new MethodSignature("Any", "method", List.of()));
        context.exitMethodWithReturn("value");
        context.exitMethodWithException(new RuntimeException(), null);
        context.reset();

        var tree = context.captureTrace();
        assertThat(tree.isEmpty()).isTrue();
        assertThat(tree.roots()).isEmpty();
    }

    @Test
    void isActiveReturnsFalse() {
        assertThat(NoopNarrativeContext.INSTANCE.isActive()).isFalse();
    }

    @Test
    void isSingleton() {
        assertThat(NoopNarrativeContext.INSTANCE).isSameAs(NoopNarrativeContext.INSTANCE);
    }

    @Test
    void noopWrapRunnableDelegatesDirectly() {
        var context = NoopNarrativeContext.INSTANCE;
        var snapshot = context.snapshot();

        boolean[] ran = {false};
        Runnable original = () -> ran[0] = true;
        var wrapped = snapshot.wrap(original);
        wrapped.run();

        assertThat(ran[0]).isTrue();
    }

    @Test
    void snapshotReturnsNonNullAndActivateReturnsScope() {
        var context = NoopNarrativeContext.INSTANCE;

        var snapshot = context.snapshot();
        assertThat(snapshot).isNotNull();

        var scope = snapshot.activate();
        assertThat(scope).isNotNull();
        scope.close(); // should not throw
    }
}
