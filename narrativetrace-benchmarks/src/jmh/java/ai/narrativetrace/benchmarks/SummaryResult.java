package ai.narrativetrace.benchmarks;

import ai.narrativetrace.core.annotation.NarrativeSummary;

public record SummaryResult(String value) {
    @NarrativeSummary
    public String summary() {
        return "Result: " + value;
    }
}
