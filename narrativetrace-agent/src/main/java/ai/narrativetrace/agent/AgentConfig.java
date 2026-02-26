package ai.narrativetrace.agent;

import ai.narrativetrace.core.config.ConfigResolver;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgentConfig(List<String> packages) {

    private static final int KEY_VALUE_PAIR = 2;

    public static AgentConfig parse(String agentArgs) {
        return parse(agentArgs, Thread.currentThread().getContextClassLoader());
    }

    public static AgentConfig parse(String agentArgs, ClassLoader classLoader) {
        if (agentArgs != null && !agentArgs.isBlank()) {
            var config = parseKeyValuePairs(agentArgs);
            var packages = parsePackages(config.getOrDefault("packages", ""));
            return new AgentConfig(packages);
        }
        var resolver = new ConfigResolver(classLoader);
        var packagesValue = resolver.resolve("narrativetrace.packages", "");
        return new AgentConfig(parsePackages(packagesValue));
    }

    private static Map<String, String> parseKeyValuePairs(String agentArgs) {
        var result = new LinkedHashMap<String, String>();
        for (var part : agentArgs.split(",")) {
            var entry = parseEntry(part);
            if (result.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("Duplicate agent config key: " + entry.getKey());
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Map.Entry<String, String> parseEntry(String part) {
        var kv = part.split("=", KEY_VALUE_PAIR);
        if (kv.length != KEY_VALUE_PAIR) {
            throw new IllegalArgumentException("Invalid agent config entry: " + part.trim());
        }
        return Map.entry(kv[0].trim(), kv[1].trim());
    }

    private static List<String> parsePackages(String value) {
        return Arrays.stream(value.split(";"))
                .filter(s -> !s.isBlank())
                .map(AgentConfig::normalizePackage)
                .toList();
    }

    static String normalizePackage(String pkg) {
        var normalized = pkg.trim();
        if (normalized.endsWith(".**")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        } else if (normalized.endsWith(".*")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        normalized = normalized.replace('.', '/');
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    public boolean shouldTransform(String className) {
        if (packages.isEmpty()) return false;
        for (var pkg : packages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
