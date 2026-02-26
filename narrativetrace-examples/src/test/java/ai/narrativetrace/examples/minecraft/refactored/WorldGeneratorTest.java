package ai.narrativetrace.examples.minecraft.refactored;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldGeneratorTest {

    @Test
    void generateChunkReturnsChunkWithCoordinates() {
        WorldGenerator generator = new DefaultWorldGenerator();

        Chunk chunk = generator.generateChunk(3, 7);

        assertThat(chunk.x()).isEqualTo(3);
        assertThat(chunk.z()).isEqualTo(7);
        assertThat(chunk.biome()).isNotBlank();
    }
}
