package ai.narrativetrace.servlet;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.export.RequestContext;
import ai.narrativetrace.core.export.TraceExporter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class NarrativeTraceFilter implements Filter {

    private final NarrativeContext context;
    private final TraceExporter exporter;

    public NarrativeTraceFilter(NarrativeContext context, TraceExporter exporter) {
        this.context = context;
        this.exporter = exporter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }
        context.reset();
        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            var tree = context.captureTrace();
            try {
                if (!tree.isEmpty()) {
                    long duration = System.currentTimeMillis() - startTime;
                    var requestContext = new RequestContext(
                            httpRequest.getMethod(),
                            httpRequest.getRequestURI(),
                            httpResponse.getStatus(),
                            duration);
                    exporter.export(tree, requestContext);
                }
            } finally {
                context.reset();
            }
        }
    }
}
