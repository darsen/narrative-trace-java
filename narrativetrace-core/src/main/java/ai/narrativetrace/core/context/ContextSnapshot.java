package ai.narrativetrace.core.context;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface ContextSnapshot {

    ContextScope activate();

    default Runnable wrap(Runnable task) {
        return () -> {
            try (var scope = activate()) {
                task.run();
            }
        };
    }

    default <T> Callable<T> wrap(Callable<T> task) {
        return () -> {
            try (var scope = activate()) {
                return task.call();
            }
        };
    }

    default <T> Supplier<T> wrap(Supplier<T> task) {
        return () -> {
            try (var scope = activate()) {
                return task.get();
            }
        };
    }
}
