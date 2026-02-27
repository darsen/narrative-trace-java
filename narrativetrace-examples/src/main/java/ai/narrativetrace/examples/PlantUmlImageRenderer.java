package ai.narrativetrace.examples;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public final class PlantUmlImageRenderer {

  public static void renderDirectory(Path directory) throws IOException {
    try (Stream<Path> files = Files.walk(directory)) {
      var pumlFiles = files.filter(p -> p.toString().endsWith(".puml")).toList();
      for (var puml : pumlFiles) {
        renderFile(puml);
      }
    }
  }

  private static void renderFile(Path pumlFile) throws IOException {
    var source = Files.readString(pumlFile);
    var svgFile =
        pumlFile.resolveSibling(pumlFile.getFileName().toString().replaceAll("\\.puml$", ".svg"));
    var reader = new SourceStringReader(source);
    try (OutputStream os = Files.newOutputStream(svgFile)) {
      reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: PlantUmlImageRenderer <directory>");
      System.exit(1);
    }
    var directory = Path.of(args[0]);
    renderDirectory(directory);
    try (Stream<Path> files = Files.walk(directory)) {
      var svgFiles = files.filter(p -> p.toString().endsWith(".svg")).toList();
      System.out.println("\n--- SVG files rendered (" + svgFiles.size() + ") ---");
      for (var svg : svgFiles) {
        System.out.println("  " + svg.toAbsolutePath());
      }
      System.out.println("---");
    }
  }
}
