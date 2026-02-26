package ai.narrativetrace.diagrams;

import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.TraceTree;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class PlantUmlSequenceDiagramRenderer {

    public String render(TraceTree tree) {
        var sb = new StringBuilder();
        sb.append("@startuml\n");

        for (var participant : collectParticipants(tree.roots())) {
            sb.append("participant ").append(quoteIfNeeded(participant)).append("\n");
        }

        for (var root : tree.roots()) {
            renderNode(root, root.signature().className(), sb);
        }

        sb.append("@enduml");
        return sb.toString().stripTrailing();
    }

    private void renderNode(TraceNode node, String caller, StringBuilder sb) {
        var target = node.signature().className();
        var method = node.signature().methodName();
        var params = node.signature().parameters().stream()
                .map(p -> p.name())
                .collect(Collectors.joining(", "));

        sb.append(quoteIfNeeded(caller)).append(" -> ").append(quoteIfNeeded(target))
                .append(": ").append(method).append("(").append(params).append(")\n");

        for (var child : node.children()) {
            renderNode(child, target, sb);
        }

        var outcome = node.outcome();
        if (outcome instanceof TraceOutcome.Returned returned) {
            sb.append(quoteIfNeeded(target)).append(" --> ").append(quoteIfNeeded(caller))
                    .append(": ").append(returned.renderedValue()).append("\n");
        } else if (outcome instanceof TraceOutcome.Threw threw) {
            sb.append(quoteIfNeeded(target)).append(" -[#red]-> ").append(quoteIfNeeded(caller))
                    .append(": ").append(threw.exception().getClass().getSimpleName()).append("\n");
        }
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
