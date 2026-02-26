package ai.narrativetrace.examples.clarity;

public class DefaultGuestRepository implements GuestRepository {

    @Override
    public Guest findGuestById(String guestId) {
        return new Guest(guestId, "Jane Smith", "jane@example.com");
    }

    @Override
    public String renderReport() {
        return "<html><body>Guest Report</body></html>";
    }

    @Override
    public boolean dispatchEmail(String guestId, String message) {
        return true;
    }
}
