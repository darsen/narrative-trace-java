package ai.narrativetrace.examples.minecraft.unrefactored;

public class DefaultThingFactory implements ThingFactory {

  @Override
  public int create(int type) {
    return type;
  }
}
