package ai.narrativetrace.core.export;

import ai.narrativetrace.core.tree.TraceTree;

@FunctionalInterface
public interface TraceExporter {

    void export(TraceTree tree, RequestContext requestContext);
}
