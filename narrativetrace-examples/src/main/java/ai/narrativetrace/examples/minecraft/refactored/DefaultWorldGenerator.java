package ai.narrativetrace.examples.minecraft.refactored;

public class DefaultWorldGenerator implements WorldGenerator {

  @Override
  public Chunk generateChunk(int x, int z) {
    return new Chunk(x, z, "plains");
  }
}
