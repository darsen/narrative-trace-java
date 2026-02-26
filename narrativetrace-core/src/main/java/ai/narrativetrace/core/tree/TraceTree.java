package ai.narrativetrace.core.tree;

import ai.narrativetrace.core.event.TraceNode;

import java.util.List;

public interface TraceTree {

    List<TraceNode> roots();

    boolean isEmpty();
}
