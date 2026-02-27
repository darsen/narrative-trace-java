package ai.narrativetrace.spring;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.proxy.NarrativeTraceProxy;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/** BeanPostProcessor that wraps eligible beans in tracing proxies at HIGHEST_PRECEDENCE. */
public class NarrativeTraceBeanPostProcessor
    implements BeanPostProcessor, Ordered, BeanFactoryAware {

  private final List<String> basePackages;
  private NarrativeContext context;
  private BeanFactory beanFactory;

  public NarrativeTraceBeanPostProcessor(List<String> basePackages) {
    this.basePackages = basePackages;
  }

  public NarrativeTraceBeanPostProcessor(NarrativeContext context, List<String> basePackages) {
    this.context = context;
    this.basePackages = basePackages;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    var beanClass = bean.getClass();

    if (!shouldWrap(beanClass)) {
      return bean;
    }

    var interfaces = beanClass.getInterfaces();
    if (interfaces.length == 0) {
      return bean;
    }

    var tracingInterfaces = findTracingInterfaces(interfaces);
    if (tracingInterfaces.length == 0) {
      return bean;
    }
    if (tracingInterfaces.length == 1) {
      @SuppressWarnings("unchecked")
      var iface = (Class<Object>) tracingInterfaces[0];
      return NarrativeTraceProxy.trace(bean, iface, context());
    }
    return NarrativeTraceProxy.trace(bean, tracingInterfaces, context());
  }

  private NarrativeContext context() {
    if (context == null) {
      context = beanFactory.getBean(NarrativeContext.class);
    }
    return context;
  }

  private Class<?>[] findTracingInterfaces(Class<?>[] interfaces) {
    return java.util.Arrays.stream(interfaces)
        .filter(iface -> basePackages.stream().anyMatch(iface.getPackageName()::startsWith))
        .toArray(Class<?>[]::new);
  }

  private boolean shouldWrap(Class<?> beanClass) {
    if (basePackages.isEmpty()) return false;
    var packageName = beanClass.getPackageName();
    return basePackages.stream().anyMatch(packageName::startsWith);
  }
}
