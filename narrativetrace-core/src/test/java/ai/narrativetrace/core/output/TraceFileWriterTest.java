package ai.narrativetrace.core.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFileWriterTest {

    @Test
    void createsParentDirectoriesWhenWriting(@TempDir Path tempDir) throws IOException {
        var writer = new TraceFileWriter();
        var content = "# Trace\n";
        var file = tempDir.resolve("traces/OrderServiceTest/customer_places_order.md");

        assertThat(file.getParent()).doesNotExist();

        writer.write(content, file);

        assertThat(file).exists();
        assertThat(Files.readString(file)).isEqualTo(content);
    }

    @Test
    void doesNotThrowWhenPathHasNoParent() throws IOException {
        var writer = new TraceFileWriter();
        var content = "# Trace\n";
        var file = Path.of("test-trace-" + System.nanoTime() + ".md");

        try {
            writer.write(content, file);
            assertThat(Files.readString(file)).isEqualTo(content);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void writesMarkdownStringToFile(@TempDir Path tempDir) throws IOException {
        var writer = new TraceFileWriter();
        var content = "---\ntype: trace\n---\n\n## Trace: OrderService.placeOrder\n";
        var file = tempDir.resolve("trace.md");

        writer.write(content, file);

        assertThat(file).exists();
        assertThat(Files.readString(file)).isEqualTo(content);
    }
}
