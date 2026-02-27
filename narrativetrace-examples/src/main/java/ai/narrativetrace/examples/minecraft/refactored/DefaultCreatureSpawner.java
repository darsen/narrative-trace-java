package ai.narrativetrace.examples.minecraft.refactored;

public class DefaultCreatureSpawner implements CreatureSpawner {

  @Override
  public Creature spawnHostile(CreatureType type, int x, int y, int z) {
    return new Creature(type, x, y, z);
  }
}
