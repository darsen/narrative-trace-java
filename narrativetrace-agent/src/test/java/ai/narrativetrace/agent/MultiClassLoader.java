package ai.narrativetrace.agent;

import java.util.HashMap;
import java.util.Map;

public class MultiClassLoader extends ClassLoader {

  private final Map<String, byte[]> classes = new HashMap<>();

  public MultiClassLoader(ClassLoader parent) {
    super(parent);
  }

  public void addClass(String name, byte[] bytes) {
    classes.put(name, bytes);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (classes.containsKey(name)) {
      var bytes = classes.get(name);
      var clazz = defineClass(name, bytes, 0, bytes.length);
      if (resolve) {
        resolveClass(clazz);
      }
      return clazz;
    }
    return super.loadClass(name, resolve);
  }
}
