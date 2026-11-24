package BaseSkeletonForCatalogTypeSystem.Examples;

import java.util.List;
import java.util.Set;

public class ElevatorManagementSkeleton {

    // ============================================================
    // 3. ELEVATOR MANAGEMENT SYSTEM
    // ============================================================

    enum ElevatorState { IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN, OUT_OF_SERVICE }
    enum Direction { UP, DOWN, NONE }

    static class Elevator {
        private String id;
        private int currentFloor;
        private ElevatorState state;
        private Direction direction;
        private Set<Integer> requestedFloors;
    }

    static class ElevatorRequest {
        private int sourceFloor;
        private Integer destinationFloor;
        private Direction direction;
    }

    interface ElevatorSchedulingStrategy {
        Elevator selectElevator(ElevatorRequest request, List<Elevator> elevators);
    }

    interface ElevatorController {
        void submitRequest(ElevatorRequest request);
        void moveToNextFloor(String elevatorId);
        void openDoor(String elevatorId);
        void closeDoor(String elevatorId);
    }

    interface ElevatorService {
        void requestElevator(int sourceFloor, Direction direction);
        void selectFloor(String elevatorId, int destinationFloor);
    }
}
