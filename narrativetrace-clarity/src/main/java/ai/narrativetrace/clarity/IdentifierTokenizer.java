package ai.narrativetrace.clarity;

import java.util.List;
import java.util.regex.Pattern;

public final class IdentifierTokenizer {

    private static final Pattern SPLIT_PATTERN = Pattern.compile(
            "(?<=[a-z])(?=[A-Z])"
                    + "|(?<=[A-Z])(?=[A-Z][a-z])"
                    + "|(?<=[a-zA-Z])(?=[0-9])"
                    + "|(?<=[0-9])(?=[a-zA-Z])"
                    + "|_"
    );

    public List<String> tokenize(String identifier) {
        return List.of(SPLIT_PATTERN.split(identifier)).stream()
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .toList();
    }
}
