package ai.narrativetrace.spring.test;

import java.io.Serializable;

public class MultiInterfaceService implements Serializable, AuditService {

    @Override
    public String audit(String action) {
        return "audited: " + action;
    }
}
