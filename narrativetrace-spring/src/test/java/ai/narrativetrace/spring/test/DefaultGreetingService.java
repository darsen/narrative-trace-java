package ai.narrativetrace.spring.test;

public class DefaultGreetingService implements GreetingService {

  @Override
  public String greet(String name) {
    return "Hello, " + name + "!";
  }
}
