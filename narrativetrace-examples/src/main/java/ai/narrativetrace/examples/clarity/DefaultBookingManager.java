package ai.narrativetrace.examples.clarity;

public class DefaultBookingManager implements BookingManager {

    @Override
    public String handleBooking(String name, String type, String d1, String d2) {
        return "Booking confirmed for " + name + " (" + type + ") " + d1 + " to " + d2;
    }
}
