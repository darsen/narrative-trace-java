package ai.narrativetrace.core.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TraceFileWriter {

    public void write(String content, Path file) throws IOException {
        var parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, content);
    }
}
