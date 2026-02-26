package ai.narrativetrace.clarity;

import java.util.List;

public final class ParameterNameScorer {

    private final IdentifierTokenizer tokenizer = new IdentifierTokenizer();
    private final GenericTokenDetector genericDetector = new GenericTokenDetector();
    private final AbbreviationDictionary abbreviationDictionary = new AbbreviationDictionary();

    public double score(String paramName) {
        var tokens = tokenizer.tokenize(paramName);
        if (tokens.isEmpty()) return 0.0;

        if (tokens.size() == 1) {
            return scoreSingleToken(tokens.get(0));
        }
        return scoreMultiToken(tokens);
    }

    private double scoreSingleToken(String token) {
        var generic = genericDetector.detect(token);
        return switch (generic.tier()) {
            case MEANINGLESS -> 0.0;
            case VAGUE -> 0.10;
            case TYPED_GENERIC -> 0.50;
            case NOT_GENERIC -> {
                var abbr = abbreviationDictionary.lookup(token);
                yield abbr != null ? 0.40 : 0.80;
            }
        };
    }

    private double scoreMultiToken(List<String> tokens) {
        boolean hasMeaningless = tokens.stream()
                .anyMatch(t -> genericDetector.detect(t).tier() == GenericTokenDetector.Tier.MEANINGLESS);
        if (hasMeaningless) return 0.15;

        double avgGeneric = tokens.stream()
                .mapToDouble(t -> {
                    var tier = genericDetector.detect(t).tier();
                    if (tier == GenericTokenDetector.Tier.TYPED_GENERIC) return 0.9;
                    return genericDetector.detect(t).score();
                })
                .average()
                .orElse(0.0);

        double abbreviation = scoreAbbreviations(tokens);

        boolean hasDomainToken = tokens.stream()
                .anyMatch(t -> genericDetector.detect(t).tier() == GenericTokenDetector.Tier.NOT_GENERIC);
        double domainBonus = hasDomainToken ? 1.0 : 0.7;

        return clamp(avgGeneric * 0.45 + abbreviation * 0.25 + domainBonus * 0.30);
    }

    private double scoreAbbreviations(List<String> tokens) {
        return tokens.stream()
                .mapToDouble(t -> {
                    var entry = abbreviationDictionary.lookup(t);
                    if (entry == null) return 1.0;
                    return switch (entry.tier()) {
                        case UNIVERSAL -> 1.0;
                        case WELL_KNOWN -> 0.8;
                        case AMBIGUOUS -> 0.5;
                    };
                })
                .average()
                .orElse(1.0);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
