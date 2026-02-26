package ai.narrativetrace.core.render;

import ai.narrativetrace.core.tree.TraceTree;

public interface NarrativeRenderer {

    String render(TraceTree tree);
}
