package ai.narrativetrace.examples.minecraft.refactored;

public interface WorldGenerator {
  Chunk generateChunk(int x, int z);
}
