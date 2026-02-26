package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClarityScannerTest {

    @Test
    void scanReturnsResultForEachClass() {
        var scanner = new ClarityScanner();
        var results = scanner.scan(List.of(SampleService.class));

        assertThat(results).containsKey("SampleService");
        assertThat(results.get("SampleService").overallScore()).isBetween(0.0, 1.0);
    }

    @Test
    void scanMultipleClasses() {
        var scanner = new ClarityScanner();
        var results = scanner.scan(List.of(SampleService.class, InventoryService.class));

        assertThat(results).containsKeys("SampleService", "InventoryService");
    }

    @Test
    void scanSkipsClassesWithNoPublicMethods() {
        var scanner = new ClarityScanner();
        var results = scanner.scan(List.of(PrivateOnly.class));

        assertThat(results).isEmpty();
    }

    @Test
    void scanSkipsObjectMethods() {
        var scanner = new ClarityScanner();
        var results = scanner.scan(List.of(SampleService.class));

        var result = results.get("SampleService");
        assertThat(result.issues().stream().map(ClarityIssue::element))
                .doesNotContain("SampleService.toString", "SampleService.hashCode", "SampleService.equals");
    }

    @Test
    void scanEmptyListReturnsEmptyMap() {
        var scanner = new ClarityScanner();
        var results = scanner.scan(List.of());

        assertThat(results).isEmpty();
    }

    @Test
    void scanPathLoadsClassesFromDirectory(@TempDir Path tempDir) throws Exception {
        compileToDir(tempDir);
        var scanner = new ClarityScanner();
        var results = scanner.scan(tempDir);

        assertThat(results).isNotEmpty();
    }

    @Test
    void scanPathSkipsEmptyDirectory(@TempDir Path tempDir) throws IOException {
        var scanner = new ClarityScanner();
        var results = scanner.scan(tempDir);

        assertThat(results).isEmpty();
    }

    private void compileToDir(Path dir) throws Exception {
        // Copy a compiled .class file from the test classpath into the temp dir
        var className = SampleService.class.getName();
        var classFile = className.replace('.', '/') + ".class";
        try (var is = getClass().getClassLoader().getResourceAsStream(classFile)) {
            assertThat(is).isNotNull();
            var target = dir.resolve(classFile);
            Files.createDirectories(target.getParent());
            Files.copy(is, target);
        }
    }

    @Test
    void mainWritesReportAndJson(@TempDir Path tempDir) throws Exception {
        var classesDir = tempDir.resolve("classes");
        compileToDir(classesDir);
        var outputDir = tempDir.resolve("output");

        ClarityScannerMain.main(new String[]{
                "--classes-dir", classesDir.toString(),
                "--output-dir", outputDir.toString()
        });

        assertThat(outputDir.resolve("clarity-report.md")).exists();
        assertThat(outputDir.resolve("clarity-results.json")).exists();
    }

    @Test
    void mainDefaultsFormatToBoth(@TempDir Path tempDir) throws Exception {
        var classesDir = tempDir.resolve("classes");
        compileToDir(classesDir);
        var outputDir = tempDir.resolve("output");

        ClarityScannerMain.main(new String[]{
                "--classes-dir", classesDir.toString(),
                "--output-dir", outputDir.toString(),
                "--format", "both"
        });

        assertThat(outputDir.resolve("clarity-report.md")).exists();
        assertThat(outputDir.resolve("clarity-results.json")).exists();
    }

    @Test
    void mainFormatMdWritesOnlyMarkdown(@TempDir Path tempDir) throws Exception {
        var classesDir = tempDir.resolve("classes");
        compileToDir(classesDir);
        var outputDir = tempDir.resolve("output");

        ClarityScannerMain.main(new String[]{
                "--classes-dir", classesDir.toString(),
                "--output-dir", outputDir.toString(),
                "--format", "md"
        });

        assertThat(outputDir.resolve("clarity-report.md")).exists();
        assertThat(outputDir.resolve("clarity-results.json")).doesNotExist();
    }

    @Test
    void mainFormatJsonWritesOnlyJson(@TempDir Path tempDir) throws Exception {
        var classesDir = tempDir.resolve("classes");
        compileToDir(classesDir);
        var outputDir = tempDir.resolve("output");

        ClarityScannerMain.main(new String[]{
                "--classes-dir", classesDir.toString(),
                "--output-dir", outputDir.toString(),
                "--format", "json"
        });

        assertThat(outputDir.resolve("clarity-report.md")).doesNotExist();
        assertThat(outputDir.resolve("clarity-results.json")).exists();
    }

    @SuppressWarnings("unused")
    public static class SampleService {
        public void placeOrder(String orderId) {}
        public String findCustomer(String customerId) { return ""; }
    }

    @SuppressWarnings("unused")
    public static class InventoryService {
        public boolean checkStock(String productId) { return true; }
    }

    @SuppressWarnings("unused")
    static class PrivateOnly {
        private void secretMethod() {}
    }
}
