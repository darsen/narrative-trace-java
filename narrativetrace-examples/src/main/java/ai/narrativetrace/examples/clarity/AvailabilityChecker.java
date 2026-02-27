package ai.narrativetrace.examples.clarity;

import java.util.List;

public interface AvailabilityChecker {
  List<Room> findAvailableRooms(String roomCategory, DateRange dateRange);
}
