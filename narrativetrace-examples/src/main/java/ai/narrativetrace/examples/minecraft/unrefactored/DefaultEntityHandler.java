package ai.narrativetrace.examples.minecraft.unrefactored;

public class DefaultEntityHandler implements EntityHandler {

    @Override
    public Entity execute(int kind, int a, int b, int c) {
        return new Entity(kind, a, b, c);
    }
}
