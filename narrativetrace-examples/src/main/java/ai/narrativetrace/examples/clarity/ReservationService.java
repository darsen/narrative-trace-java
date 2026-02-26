package ai.narrativetrace.examples.clarity;

import java.time.LocalDate;

public interface ReservationService {
    Reservation confirmReservation(String guestId, String roomCategory,
                                   LocalDate checkInDate, LocalDate checkOutDate);
}
