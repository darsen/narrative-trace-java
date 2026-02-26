package ai.narrativetrace.examples.minecraft;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class MinecraftExampleTest {

    @Test
    void mainRunsBothRefactoredAndUnrefactoredScenarios() {
        var out = new ByteArrayOutputStream();
        var original = System.out;
        System.setOut(new PrintStream(out));
        try {
            MinecraftExample.main(new String[]{});
        } finally {
            System.setOut(original);
        }

        var output = out.toString();
        assertThat(output).contains("Refactored: Player Joins World");
        assertThat(output).contains("Unrefactored: Player Joins World");
        assertThat(output).contains("WorldServer.playerJoined");
        assertThat(output).contains("GameManager.handle");
        assertThat(output).contains("sequenceDiagram");
    }

    @Test
    void classCanBeInstantiated() {
        var example = new MinecraftExample();
        assertThat(example).isNotNull();
    }
}
