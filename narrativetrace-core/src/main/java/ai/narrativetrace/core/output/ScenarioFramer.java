package ai.narrativetrace.core.output;

/** Humanizes method and class names into readable scenario titles for trace output. */
public final class ScenarioFramer {

  private ScenarioFramer() {}

  public static String frame(String displayName) {
    return "Scenario: " + humanize(displayName);
  }

  public static String humanize(String displayName) {
    var name = displayName.replaceAll("\\([^)]*\\)$", "");
    if (name.isEmpty()) {
      return "";
    }
    if (name.contains(" ")) {
      return name;
    }
    var words = name.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
    return Character.toUpperCase(words.charAt(0)) + words.substring(1);
  }
}
