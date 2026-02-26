package ai.narrativetrace.core.output;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OutputDirectoryResolverTest {

    @Test
    void computesTraceDirectoryFromTestClassName() {
        var resolver = new OutputDirectoryResolver(Path.of("target/narrativetrace"));

        var dir = resolver.traceDirectory("ai.narrativetrace.examples.ecommerce.OrderServiceTest");

        assertThat(dir).isEqualTo(Path.of("target/narrativetrace/traces/OrderServiceTest"));
    }

    @Test
    void computesTraceFilePathWithSluggedMethodName() {
        var resolver = new OutputDirectoryResolver(Path.of("target/narrativetrace"));

        var file = resolver.traceFile(
                "ai.narrativetrace.examples.ecommerce.OrderServiceTest",
                "customerPlacesOrderSuccessfully");

        assertThat(file).isEqualTo(Path.of(
                "target/narrativetrace/traces/OrderServiceTest/customer_places_order_successfully.md"));
    }

    @Test
    void preservesUnderscoreMethodNames() {
        var resolver = new OutputDirectoryResolver(Path.of("target/narrativetrace"));

        var file = resolver.traceFile("OrderServiceTest", "customer_places_order");

        assertThat(file).isEqualTo(Path.of(
                "target/narrativetrace/traces/OrderServiceTest/customer_places_order.md"));
    }

    @Test
    void computesTraceDirectoryFromSimpleClassName() {
        var resolver = new OutputDirectoryResolver(Path.of("build/narrativetrace"));

        var dir = resolver.traceDirectory("OrderServiceTest");

        assertThat(dir).isEqualTo(Path.of("build/narrativetrace/traces/OrderServiceTest"));
    }

    @Test
    void baseDirReturnsConfiguredPath() {
        var resolver = new OutputDirectoryResolver(Path.of("target/narrativetrace"));

        assertThat(resolver.baseDir()).isEqualTo(Path.of("target/narrativetrace"));
    }
}
