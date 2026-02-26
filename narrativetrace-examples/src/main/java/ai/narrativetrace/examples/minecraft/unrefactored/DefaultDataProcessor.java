package ai.narrativetrace.examples.minecraft.unrefactored;

public class DefaultDataProcessor implements DataProcessor {

    @Override
    public DataResult process(int a, int b) {
        return new DataResult(a, b, "plains");
    }
}
