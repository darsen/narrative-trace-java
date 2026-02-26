package ai.narrativetrace.servlet;

import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.export.RequestContext;
import ai.narrativetrace.core.tree.TraceTree;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NarrativeTraceFilterTest {

    private final ThreadLocalNarrativeContext context = new ThreadLocalNarrativeContext();

    @Test
    void implementsFilter() {
        var filter = new NarrativeTraceFilter(context, (tree, req) -> { });

        assertThat(filter).isInstanceOf(Filter.class);
    }

    @Test
    void callsChainDoFilter() throws Exception {
        var filter = new NarrativeTraceFilter(context, (tree, req) -> { });
        var request = new StubHttpServletRequest("GET", "/api/test");
        var response = new StubHttpServletResponse();
        var chainRequest = new AtomicReference<ServletRequest>();
        FilterChain chain = (req, res) -> chainRequest.set(req);

        filter.doFilter(request, response, chain);

        assertThat(chainRequest.get()).isSameAs(request);
    }

    @Test
    void resetsContextBeforeRequest() throws Exception {
        context.enterMethod(new MethodSignature("Stale", "leftover", List.of()));
        context.exitMethodWithReturn("\"stale\"");

        var contextEmptyInsideChain = new AtomicBoolean(false);
        FilterChain chain = (req, res) ->
                contextEmptyInsideChain.set(context.captureTrace().isEmpty());

        var filter = new NarrativeTraceFilter(context, (tree, req) -> { });
        filter.doFilter(new StubHttpServletRequest(), new StubHttpServletResponse(), chain);

        assertThat(contextEmptyInsideChain).isTrue();
    }

    @Test
    void resetsContextAfterRequest() throws Exception {
        FilterChain chain = (req, res) -> {
            context.enterMethod(new MethodSignature("Svc", "handle", List.of()));
            context.exitMethodWithReturn("\"ok\"");
        };

        var filter = new NarrativeTraceFilter(context, (tree, req) -> { });
        filter.doFilter(new StubHttpServletRequest(), new StubHttpServletResponse(), chain);

        assertThat(context.captureTrace().isEmpty()).isTrue();
    }

    @Test
    void capturesTraceAndCallsExporter() throws Exception {
        var exportedTrees = new ArrayList<TraceTree>();
        FilterChain chain = (req, res) -> {
            context.enterMethod(new MethodSignature("Svc", "handle", List.of()));
            context.exitMethodWithReturn("\"ok\"");
        };

        var filter = new NarrativeTraceFilter(context, (tree, reqCtx) -> exportedTrees.add(tree));
        filter.doFilter(new StubHttpServletRequest(), new StubHttpServletResponse(), chain);

        assertThat(exportedTrees).hasSize(1);
        assertThat(exportedTrees.get(0).isEmpty()).isFalse();
    }

    @Test
    void requestContextHasCorrectMethodUriAndStatus() throws Exception {
        var exportedContexts = new ArrayList<RequestContext>();
        FilterChain chain = (req, res) -> {
            context.enterMethod(new MethodSignature("Svc", "create", List.of()));
            context.exitMethodWithReturn("\"created\"");
            ((StubHttpServletResponse) res).setStatus(201);
        };

        var filter = new NarrativeTraceFilter(context, (tree, reqCtx) -> exportedContexts.add(reqCtx));
        var request = new StubHttpServletRequest("POST", "/api/orders");
        var response = new StubHttpServletResponse();
        filter.doFilter(request, response, chain);

        assertThat(exportedContexts).hasSize(1);
        var reqCtx = exportedContexts.get(0);
        assertThat(reqCtx.method()).isEqualTo("POST");
        assertThat(reqCtx.uri()).isEqualTo("/api/orders");
        assertThat(reqCtx.statusCode()).isEqualTo(201);
    }

    @Test
    void requestContextHasNonNegativeDuration() throws Exception {
        var exportedContexts = new ArrayList<RequestContext>();
        FilterChain chain = (req, res) -> {
            context.enterMethod(new MethodSignature("Svc", "handle", List.of()));
            context.exitMethodWithReturn("\"ok\"");
        };

        var filter = new NarrativeTraceFilter(context, (tree, reqCtx) -> exportedContexts.add(reqCtx));
        filter.doFilter(new StubHttpServletRequest(), new StubHttpServletResponse(), chain);

        assertThat(exportedContexts.get(0).durationMillis()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void skipsExportForEmptyTraces() throws Exception {
        var exportCalled = new AtomicBoolean(false);
        var filter = new NarrativeTraceFilter(context, (tree, reqCtx) -> exportCalled.set(true));

        filter.doFilter(new StubHttpServletRequest(), new StubHttpServletResponse(),
                (req, res) -> { });

        assertThat(exportCalled).isFalse();
    }

    @Test
    void handlesNonHttpRequests() throws Exception {
        var exportCalled = new AtomicBoolean(false);
        var chainCalled = new AtomicBoolean(false);
        var filter = new NarrativeTraceFilter(context, (tree, reqCtx) -> exportCalled.set(true));

        ServletRequest plainRequest = new StubServletRequest();
        ServletResponse plainResponse = new StubServletResponse();
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(plainRequest, plainResponse, chain);

        assertThat(chainCalled).isTrue();
        assertThat(exportCalled).isFalse();
    }

    @Test
    void cleansUpWhenChainThrows() {
        context.enterMethod(new MethodSignature("Stale", "leftover", List.of()));
        context.exitMethodWithReturn("\"stale\"");

        FilterChain chain = (req, res) -> {
            context.enterMethod(new MethodSignature("Svc", "handle", List.of()));
            context.exitMethodWithReturn("\"ok\"");
            throw new RuntimeException("chain error");
        };

        var filter = new NarrativeTraceFilter(context, (tree, reqCtx) -> { });

        assertThatThrownBy(() ->
                filter.doFilter(new StubHttpServletRequest(), new StubHttpServletResponse(), chain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("chain error");

        assertThat(context.captureTrace().isEmpty()).isTrue();
    }
}
