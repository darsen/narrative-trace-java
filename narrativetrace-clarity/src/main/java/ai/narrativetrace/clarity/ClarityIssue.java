package ai.narrativetrace.clarity;

public record ClarityIssue(
        String category,
        String element,
        String suggestion,
        Severity severity,
        int occurrences,
        double impactScore
) {
    public enum Severity {
        HIGH(3), MEDIUM(2), LOW(1);

        private final int weight;

        Severity(int weight) {
            this.weight = weight;
        }

        public int weight() {
            return weight;
        }
    }

    public ClarityIssue(String category, String element, String suggestion) {
        this(category, element, suggestion, Severity.MEDIUM, 1, Severity.MEDIUM.weight());
    }

    public ClarityIssue withOccurrences(int occurrences) {
        return new ClarityIssue(category, element, suggestion, severity, occurrences,
                severity.weight() * occurrences);
    }
}
