package ai.narrativetrace.examples.junit4;

public class DefaultGreetingService implements GreetingService {

    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
