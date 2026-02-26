package ai.narrativetrace.spring;

import ai.narrativetrace.core.context.NarrativeContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class EnableNarrativeTraceTest {

    @Configuration
    @EnableNarrativeTrace(basePackages = "ai.narrativetrace.spring.test")
    static class TestConfig {
    }

    @Test
    void enableNarrativeTraceRegistersContextBean() {
        try (var ctx = new AnnotationConfigApplicationContext(TestConfig.class)) {
            assertThat(ctx.getBean(NarrativeContext.class)).isNotNull();
        }
    }

    @Test
    void enableNarrativeTraceRegistersBeanPostProcessor() {
        try (var ctx = new AnnotationConfigApplicationContext(TestConfig.class)) {
            assertThat(ctx.getBean(NarrativeTraceBeanPostProcessor.class)).isNotNull();
        }
    }

    @Test
    void registrarHandlesClassWithoutAnnotation() {
        var registrar = new NarrativeTraceRegistrar();
        var registry = new DefaultListableBeanFactory();

        assertThatNoException().isThrownBy(() ->
                registrar.registerBeanDefinitions(
                        AnnotationMetadata.introspect(Object.class), registry));
    }
}
