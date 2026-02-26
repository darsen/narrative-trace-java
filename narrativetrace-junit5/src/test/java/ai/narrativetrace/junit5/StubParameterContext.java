package ai.narrativetrace.junit5;

import org.junit.jupiter.api.extension.ParameterContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

class StubParameterContext implements ParameterContext {

    private final Parameter parameter;

    StubParameterContext(Parameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public java.lang.reflect.Executable getDeclaringExecutable() {
        return parameter.getDeclaringExecutable();
    }

    @Override
    public Optional<Object> getTarget() {
        return Optional.empty();
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annotationType) {
        return parameter.isAnnotationPresent(annotationType);
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
        return Optional.ofNullable(parameter.getAnnotation(annotationType));
    }

    @Override
    public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
        return List.of(parameter.getAnnotationsByType(annotationType));
    }
}
