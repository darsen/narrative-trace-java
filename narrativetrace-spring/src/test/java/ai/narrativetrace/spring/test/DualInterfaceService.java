package ai.narrativetrace.spring.test;

public class DualInterfaceService implements GreetingService, AuditService {

    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    @Override
    public String audit(String action) {
        return "audited: " + action;
    }
}
