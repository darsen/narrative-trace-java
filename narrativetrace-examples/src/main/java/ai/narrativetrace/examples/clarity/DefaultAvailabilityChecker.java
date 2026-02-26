package ai.narrativetrace.examples.clarity;

import java.util.List;

public class DefaultAvailabilityChecker implements AvailabilityChecker {

    @Override
    public List<Room> findAvailableRooms(String roomCategory, DateRange dateRange) {
        return List.of(
                new Room("301", roomCategory, 189.00),
                new Room("405", roomCategory, 219.00)
        );
    }
}
