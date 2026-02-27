package ai.narrativetrace.core.output;

import java.util.List;

/** Prints per-class trace summaries with clarity scores to the console. */
public final class ConsoleSummaryReporter {

  public String formatTestResult(String testName, long durationMs) {
    return "    ✓ " + testName + " (" + durationMs + "ms)";
  }

  public String formatTestResult(String testName, long durationMs, double clarityScore) {
    return "    ✓ "
        + testName
        + " ("
        + durationMs
        + "ms, clarity: "
        + String.format("%.2f", clarityScore)
        + ")";
  }

  public String formatTestFailure(
      String testName,
      long durationMs,
      String exceptionType,
      String location,
      String traceFilePath) {
    return "    ✗ "
        + testName
        + " ("
        + durationMs
        + "ms)\n"
        + "      > "
        + exceptionType
        + " at "
        + location
        + "\n"
        + "      > Full trace: "
        + traceFilePath;
  }

  public String formatSuiteHeader() {
    return "NarrativeTrace — Recording test narratives\n";
  }

  public String formatSuiteFooter(int scenarioCount, String outputPath) {
    return "\nNarrativeTrace — Suite complete\n"
        + "  "
        + scenarioCount
        + " scenarios recorded\n"
        + "  Reports: "
        + outputPath;
  }

  public String formatSuiteFooter(
      int scenarioCount, String outputPath, List<Double> clarityScores) {
    int high = 0, moderate = 0, low = 0;
    for (var score : clarityScores) {
      if (score >= 0.7) high++;
      else if (score >= 0.4) moderate++;
      else low++;
    }
    int total = clarityScores.size();
    int highPct = Math.round(100f * high / total);
    int moderatePct = Math.round(100f * moderate / total);
    int lowPct = Math.round(100f * low / total);
    return "\nNarrativeTrace — Suite complete\n"
        + "  "
        + scenarioCount
        + " scenarios recorded\n"
        + "  Clarity: "
        + highPct
        + "% high | "
        + moderatePct
        + "% moderate | "
        + lowPct
        + "% low\n"
        + "  Reports: "
        + outputPath;
  }
}
