package ai.narrativetrace.examples.minecraft.refactored;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreatureSpawnerTest {

    @Test
    void spawnHostileReturnsCreature() {
        CreatureSpawner spawner = new DefaultCreatureSpawner();

        Creature creature = spawner.spawnHostile(CreatureType.ZOMBIE, 10, 64, 20);

        assertThat(creature.type()).isEqualTo(CreatureType.ZOMBIE);
        assertThat(creature.x()).isEqualTo(10);
        assertThat(creature.y()).isEqualTo(64);
        assertThat(creature.z()).isEqualTo(20);
    }
}
