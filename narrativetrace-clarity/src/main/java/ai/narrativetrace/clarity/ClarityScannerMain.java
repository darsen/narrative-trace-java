package ai.narrativetrace.clarity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClarityScannerMain {

    private ClarityScannerMain() {
    }

    public static void main(String[] args) {
        var classesDir = findArg(args, "--classes-dir");
        var outputDirArg = findArg(args, "--output-dir");
        var formatArg = findArg(args, "--format");

        if (classesDir == null) {
            System.err.println("--classes-dir is required");
            System.exit(1);
            return;
        }

        var outputDir = outputDirArg != null ? Path.of(outputDirArg) : Path.of("build/narrativetrace");
        var format = formatArg != null ? formatArg : "both";

        try {
            run(Path.of(classesDir), outputDir, format);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String findArg(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    static void run(Path classesDir, Path outputDir, String format) throws IOException {
        var scanner = new ClarityScanner();
        var results = scanner.scan(classesDir);

        if (results.isEmpty()) {
            System.out.println("No classes found in " + classesDir);
            return;
        }

        Files.createDirectories(outputDir);

        var writeMd = "both".equals(format) || "md".equals(format);
        var writeJson = "both".equals(format) || "json".equals(format);

        if (writeMd) {
            var report = new ClarityReportRenderer().renderSuiteReport(results);
            Files.writeString(outputDir.resolve("clarity-report.md"), report);
        }

        if (writeJson) {
            var json = new ClarityJsonExporter().export(results);
            Files.writeString(outputDir.resolve("clarity-results.json"), json);
        }

        System.out.println("Clarity analysis complete: " + results.size() + " classes scanned");
        System.out.println("Output: " + outputDir);
    }
}
