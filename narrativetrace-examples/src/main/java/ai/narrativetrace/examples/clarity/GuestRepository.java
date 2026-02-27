package ai.narrativetrace.examples.clarity;

public interface GuestRepository {
  Guest findGuestById(String guestId);

  String renderReport();

  boolean dispatchEmail(String guestId, String message);
}
