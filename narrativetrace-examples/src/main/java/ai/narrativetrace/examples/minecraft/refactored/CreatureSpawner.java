package ai.narrativetrace.examples.minecraft.refactored;

public interface CreatureSpawner {
    Creature spawnHostile(CreatureType type, int x, int y, int z);
}
