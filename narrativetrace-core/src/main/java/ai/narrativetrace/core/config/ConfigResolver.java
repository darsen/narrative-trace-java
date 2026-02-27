package ai.narrativetrace.core.config;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Resolves NarrativeTraceConfig from system properties and programmatic settings. */
public final class ConfigResolver {

  private static final String PROPERTIES_FILE = "narrativetrace.properties";

  private final Properties fileProperties;

  public ConfigResolver() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public ConfigResolver(ClassLoader classLoader) {
    this.fileProperties = loadFromClasspath(classLoader);
  }

  public String resolve(String key, String defaultValue) {
    var systemValue = System.getProperty(key);
    if (systemValue != null) {
      return systemValue;
    }
    return fileProperties.getProperty(key, defaultValue);
  }

  private static Properties loadFromClasspath(ClassLoader classLoader) {
    var props = new Properties();
    if (classLoader == null) {
      return props;
    }
    var locations = findAllLocations(classLoader);
    if (locations.isEmpty()) {
      return props;
    }
    if (locations.size() > 1) {
      throw new DuplicateConfigurationException(
          "Found multiple " + PROPERTIES_FILE + " files on classpath: " + locations);
    }
    try (var stream = locations.get(0).openStream()) {
      props.load(stream);
    } catch (IOException e) {
      // Silently fall back to defaults — file unreadable
    }
    return props;
  }

  private static List<URL> findAllLocations(ClassLoader classLoader) {
    try {
      return Collections.list(classLoader.getResources(PROPERTIES_FILE));
    } catch (IOException e) {
      return List.of();
    }
  }
}
