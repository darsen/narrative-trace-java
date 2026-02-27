package ai.narrativetrace.clarity;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Standalone clarity analyzer that scans compiled classes without running tests.
 *
 * <p>Builds synthetic trace nodes from reflection on public methods, then delegates to {@link
 * ClarityAnalyzer} for scoring. Useful for CI quality gates and IDE integration.
 *
 * <pre>{@code
 * var scanner = new ClarityScanner();
 * Map<String, ClarityResult> results = scanner.scan(Path.of("build/classes/java/main"));
 * results.forEach((cls, result) ->
 *     System.out.printf("%s: %.2f%n", cls, result.overallScore()));
 * }</pre>
 *
 * <p>Skips synthetic, bridge, private, and {@code Object} methods. Classes with missing
 * dependencies are silently skipped.
 *
 * @see ClarityAnalyzer
 * @see ClarityScannerMain
 */
public final class ClarityScanner {

  private final ClarityAnalyzer analyzer = new ClarityAnalyzer();

  /**
   * Scans all classes in the given directory and returns clarity results per class.
   *
   * @param classesDir path to compiled classes (e.g., {@code build/classes/java/main})
   * @return map of class simple name to clarity result
   * @throws IOException if the directory cannot be read
   */
  public Map<String, ClarityResult> scan(Path classesDir) throws IOException {
    var classes = new ArrayList<Class<?>>();
    var url = classesDir.toUri().toURL();
    try (var loader = new URLClassLoader(new URL[] {url}, getClass().getClassLoader());
        Stream<Path> walk = Files.walk(classesDir)) {
      walk.filter(p -> p.toString().endsWith(".class"))
          .forEach(
              p -> {
                var relative = classesDir.relativize(p).toString();
                var className =
                    relative.replace('/', '.').replace('\\', '.').replaceAll("\\.class$", "");
                try {
                  classes.add(loader.loadClass(className));
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                  // Skip classes that can't be loaded (missing dependencies)
                }
              });
    }
    return scan(classes);
  }

  /**
   * Scans the given list of classes and returns clarity results per class.
   *
   * @param classes the classes to analyze
   * @return map of class simple name to clarity result
   */
  public Map<String, ClarityResult> scan(List<Class<?>> classes) {
    var results = new LinkedHashMap<String, ClarityResult>();
    for (var clazz : classes) {
      var nodes = buildNodes(clazz);
      if (!nodes.isEmpty()) {
        var tree = new DefaultTraceTree(nodes);
        results.put(clazz.getSimpleName(), analyzer.analyze(tree));
      }
    }
    return results;
  }

  private List<TraceNode> buildNodes(Class<?> clazz) {
    var nodes = new ArrayList<TraceNode>();
    for (var method : clazz.getDeclaredMethods()) {
      if (shouldSkip(method)) {
        continue;
      }
      var params = buildParams(method);
      var signature = new MethodSignature(clazz.getSimpleName(), method.getName(), params);
      nodes.add(new TraceNode(signature, List.of(), new TraceOutcome.Returned(""), 0L));
    }
    return nodes;
  }

  private boolean shouldSkip(Method method) {
    return method.isSynthetic()
        || method.isBridge()
        || !Modifier.isPublic(method.getModifiers())
        || isObjectMethod(method);
  }

  private boolean isObjectMethod(Method method) {
    try {
      Object.class.getMethod(method.getName(), method.getParameterTypes());
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  private List<ParameterCapture> buildParams(Method method) {
    var params = new ArrayList<ParameterCapture>();
    for (var param : method.getParameters()) {
      params.add(new ParameterCapture(param.getName(), "", false));
    }
    return params;
  }
}
