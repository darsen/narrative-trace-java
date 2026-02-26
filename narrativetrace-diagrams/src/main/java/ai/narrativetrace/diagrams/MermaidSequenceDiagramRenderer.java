package ai.narrativetrace.diagrams;

import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.TraceTree;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MermaidSequenceDiagramRenderer {

    public String render(TraceTree tree) {
        var sb = new StringBuilder();
        sb.append("sequenceDiagram\n");

        for (var participant : collectParticipants(tree.roots())) {
            sb.append("    participant ").append(quoteIfNeeded(participant)).append("\n");
        }

        for (var root : tree.roots()) {
            renderNode(root, root.signature().className(), sb);
        }

        return sb.toString().stripTrailing();
    }

    private void renderNode(TraceNode node, String caller, StringBuilder sb) {
        var target = node.signature().className();
        var method = node.signature().methodName();
        var params = node.signature().parameters().stream()
                .map(p -> p.name())
                .collect(Collectors.joining(", "));

        sb.append("    ").append(quoteIfNeeded(caller)).append("->>").append(quoteIfNeeded(target))
                .append(": ").append(method).append("(").append(params).append(")\n");

        for (var child : node.children()) {
            renderNode(child, target, sb);
        }

        var outcome = node.outcome();
        if (outcome instanceof TraceOutcome.Returned returned) {
            sb.append("    ").append(quoteIfNeeded(target)).append("-->>").append(quoteIfNeeded(caller))
                    .append(": ").append(returned.renderedValue()).append("\n");
        } else if (outcome instanceof TraceOutcome.Threw threw) {
            sb.append("    ").append(quoteIfNeeded(target)).append("-x").append(quoteIfNeeded(caller))
                    .append(": ").append(threw.exception().getClass().getSimpleName()).append("\n");
        }
    }

    public String renderWithAliases(TraceTree tree) {
        var sb = new StringBuilder();
        sb.append("sequenceDiagram\n");

        var participants = collectParticipants(tree.roots());
        var aliases = buildAliases(participants);

        for (var participant : participants) {
            sb.append("    participant ").append(aliases.get(participant))
                    .append(" as ").append(participant).append("\n");
        }

        for (var root : tree.roots()) {
            renderNodeWithAliases(root, root.signature().className(), aliases, sb);
        }

        return sb.toString().stripTrailing();
    }

    private void renderNodeWithAliases(TraceNode node, String caller, Map<String, String> aliases, StringBuilder sb) {
        var target = node.signature().className();
        var method = node.signature().methodName();
        var params = node.signature().parameters().stream()
                .map(p -> p.name())
                .collect(Collectors.joining(", "));

        var callerAlias = aliases.get(caller);
        var targetAlias = aliases.get(target);

        sb.append("    ").append(callerAlias).append("->>").append(targetAlias)
                .append(": ").append(method).append("(").append(params).append(")\n");

        for (var child : node.children()) {
            renderNodeWithAliases(child, target, aliases, sb);
        }

        var outcome = node.outcome();
        if (outcome instanceof TraceOutcome.Returned returned) {
            sb.append("    ").append(targetAlias).append("-->>").append(callerAlias)
                    .append(": ").append(returned.renderedValue()).append("\n");
        } else if (outcome instanceof TraceOutcome.Threw threw) {
            sb.append("    ").append(targetAlias).append("-x").append(callerAlias)
                    .append(": ").append(threw.exception().getClass().getSimpleName()).append("\n");
        }
    }

    private Map<String, String> buildAliases(LinkedHashSet<String> participants) {
        var aliases = new LinkedHashMap<String, String>();
        var usedAliases = new LinkedHashSet<String>();
        for (var name : participants) {
            var alias = extractUpperCase(name);
            if (alias.isEmpty()) {
                alias = name;
            }
            if (usedAliases.contains(alias)) {
                int suffix = 2;
                while (usedAliases.contains(alias + suffix)) {
                    suffix++;
                }
                alias = alias + suffix;
            }
            usedAliases.add(alias);
            aliases.put(name, alias);
        }
        return aliases;
    }

    private String extractUpperCase(String name) {
        var sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append(c);
            }
        }
        return sb.length() >= 2 ? sb.substring(0, 2) : sb.toString();
    }

    private static String quoteIfNeeded(String name) {
        if (name.chars().anyMatch(c -> c == '.' || c == '-' || c == ':' || c == ' ')) {
            return "\"" + name + "\"";
        }
        return name;
    }

    private LinkedHashSet<String> collectParticipants(List<TraceNode> nodes) {
        var result = new LinkedHashSet<String>();
        for (var node : nodes) {
            result.add(node.signature().className());
            result.addAll(collectParticipants(node.children()));
        }
        return result;
    }
}
