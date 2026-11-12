import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RideBookingSkeleton {

    // ============================================================
    // 8. RIDE BOOKING SYSTEM (UBER / OLA)
    // ============================================================

    enum RideStatus {
        REQUESTED, DRIVER_ASSIGNED, DRIVER_ARRIVING,
        IN_PROGRESS, COMPLETED, CANCELLED
    }

    enum VehicleCategory { BIKE, MINI, SEDAN, SUV }

    static class Location {
        private double latitude;
        private double longitude;
    }

    static class Rider {
        private String riderId;
        private Location currentLocation;
    }

    static class Driver {
        private String driverId;
        private Location currentLocation;
        private VehicleCategory vehicleCategory;
        private boolean available;
    }

    static class Ride {
        private String rideId;
        private Rider rider;
        private Driver driver;
        private Location pickup;
        private Location destination;
        private RideStatus status;
    }

    interface DriverMatchingStrategy {
        Optional<Driver> findDriver(RideRequest request, List<Driver> availableDrivers);
    }

    interface FareCalculationStrategy {
        double calculateFare(RideRequest request);
    }

    interface RideService {
        RideRequestResult requestRide(RideRequest request);
        void updateRideStatus(String rideId, RideStatus status);
        void cancelRide(String rideId);
        Optional<Ride> getRide(String rideId);
    }

    interface LocationService {
        Location getCurrentLocation(String driverId);
        void updateLocation(String driverId, Location location);
    }

    static class RideRequest {
        private String riderId;
        private Location pickup;
        private Location destination;
        private VehicleCategory requestedCategory;
    }

    static class RideRequestResult {
        private String rideId;
        private RideStatus status;
    }
}
