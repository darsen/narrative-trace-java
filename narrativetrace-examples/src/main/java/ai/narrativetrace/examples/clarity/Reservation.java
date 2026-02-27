package ai.narrativetrace.examples.clarity;

import java.time.LocalDate;

public record Reservation(
    String reservationId,
    String guestId,
    String roomNumber,
    LocalDate checkIn,
    LocalDate checkOut) {}
