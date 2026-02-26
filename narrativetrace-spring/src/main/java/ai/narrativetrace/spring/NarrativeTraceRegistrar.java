package ai.narrativetrace.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;

public class NarrativeTraceRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        var basePackages = resolveBasePackages(metadata);

        if (!registry.containsBeanDefinition("narrativeTraceBeanPostProcessor")) {
            var bppDef = new RootBeanDefinition(NarrativeTraceBeanPostProcessor.class);
            bppDef.getConstructorArgumentValues().addIndexedArgumentValue(0, basePackages);
            bppDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition("narrativeTraceBeanPostProcessor", bppDef);
        }
    }

    private List<String> resolveBasePackages(AnnotationMetadata metadata) {
        var attrs = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EnableNarrativeTrace.class.getName()));
        if (attrs == null) {
            return List.of();
        }
        var declared = List.of(attrs.getStringArray("basePackages"));
        if (declared.isEmpty()) {
            var className = metadata.getClassName();
            var lastDot = className.lastIndexOf('.');
            return List.of(lastDot > 0 ? className.substring(0, lastDot) : className);
        }
        return declared;
    }
}
