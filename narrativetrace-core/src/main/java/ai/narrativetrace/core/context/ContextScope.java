package ai.narrativetrace.core.context;

public interface ContextScope extends AutoCloseable {
    @Override
    void close();
}
