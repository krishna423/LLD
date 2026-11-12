import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * LLD interview practice skeletons.
 *
 * Java 17+.
 * This file intentionally contains interfaces, enums, and key class outlines only.
 * Implementations and dummy data are omitted so you can practise the design.
 *
 * In IntelliJ:
 * 1. Create a Java project.
 * 2. Create LLDPracticeSkeletons.java.
 * 3. Paste this file and run the main method.
 */
public class LLDPracticeSkeletons {

    public static void main(String[] args) {
        System.out.println("LLD practice skeletons loaded. Choose a problem and design its implementation.");
    }

    // ============================================================
    // 1. PARKING LOT MANAGEMENT SYSTEM
    // ============================================================

    static class ParkingLot {
        private String id;
        private List<ParkingFloor> floors;
        private List<EntryGate> entryGates;
        private List<ExitGate> exitGates;
    }

    static class ParkingFloor {
        private String id;
        private List<ParkingSpot> spots;
    }

    static class ParkingSpot {
        private String id;
        private SpotType spotType;
        private boolean occupied;
        private Vehicle parkedVehicle;
    }

    static class Vehicle {
        private String registrationNumber;
        private VehicleType vehicleType;
    }

    static class ParkingTicket {
        private String ticketId;
        private Vehicle vehicle;
        private ParkingSpot spot;
        private Instant entryTime;
    }

    enum VehicleType { BIKE, CAR, TRUCK }
    enum SpotType { BIKE, COMPACT, LARGE }

    interface SpotAllocationStrategy {
        Optional<ParkingSpot> findSpot(Vehicle vehicle, List<ParkingFloor> floors);
    }

    interface ParkingFeeStrategy {
        double calculateFee(ParkingTicket ticket, Instant exitTime);
    }

    interface ParkingService {
        ParkingTicket parkVehicle(Vehicle vehicle);
        double unparkVehicle(String ticketId);
        Optional<ParkingTicket> findTicket(String ticketId);
    }

    interface ParkingPaymentService {
        boolean collectPayment(String ticketId, double amount);
    }

    static class EntryGate {
        private String gateId;
    }

    static class ExitGate {
        private String gateId;
    }

    // ============================================================
    // 2. PAYMENT PROCESSING SYSTEM
    // ============================================================

    enum PaymentStatus { CREATED, INITIATED, SUCCESS, FAILED, REFUNDED }
    enum PaymentMethodType { UPI, CARD, NET_BANKING }

    static class Payment {
        private String paymentId;
        private String orderId;
        private double amount;
        private PaymentStatus status;
        private PaymentMethodType methodType;
    }

    static class Refund {
        private String refundId;
        private String paymentId;
        private double amount;
    }

    interface PaymentMethod {
        PaymentResult pay(Payment payment);
    }

    interface PaymentGateway {
        PaymentResult charge(Payment payment);
        RefundResult refund(Payment payment, double amount);
    }

    interface PaymentService {
        PaymentResult processPayment(PaymentRequest request);
        RefundResult refund(String paymentId, double amount);
        Optional<Payment> getPayment(String paymentId);
    }

    interface PaymentRepository {
        void save(Payment payment);
        Optional<Payment> findById(String paymentId);
        void updateStatus(String paymentId, PaymentStatus status);
    }

    static class PaymentRequest {
        private String orderId;
        private double amount;
        private PaymentMethodType methodType;
    }

    static class PaymentResult {
        private boolean successful;
        private String transactionReference;
        private String failureReason;
    }

    static class RefundResult {
        private boolean successful;
        private String refundReference;
    }

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

    // ============================================================
    // 4. FOOD ORDERING AND DELIVERY SYSTEM
    // ============================================================

    enum OrderStatus {
        CREATED, ACCEPTED, PREPARING, READY_FOR_PICKUP,
        PICKED_UP, DELIVERED, CANCELLED
    }

    static class Restaurant {
        private String restaurantId;
        private String name;
        private List<MenuItem> menu;
    }

    static class MenuItem {
        private String itemId;
        private String name;
        private double price;
    }

    static class FoodOrder {
        private String orderId;
        private String customerId;
        private String restaurantId;
        private List<OrderLine> items;
        private OrderStatus status;
    }

    static class OrderLine {
        private MenuItem item;
        private int quantity;
    }

    static class DeliveryPartner {
        private String partnerId;
        private boolean available;
    }

    interface DeliveryAssignmentStrategy {
        Optional<DeliveryPartner> assignPartner(FoodOrder order, List<DeliveryPartner> partners);
    }

    interface DiscountStrategy {
        double calculateDiscount(FoodOrder order);
    }

    interface FoodOrderService {
        FoodOrder placeOrder(String customerId, String restaurantId, List<OrderLine> items);
        void updateOrderStatus(String orderId, OrderStatus status);
        void cancelOrder(String orderId);
        Optional<FoodOrder> getOrder(String orderId);
    }

    interface OrderStatusListener {
        void onStatusChanged(FoodOrder order, OrderStatus newStatus);
    }

    // ============================================================
    // 5. NOTIFICATION SERVICE
    // ============================================================

    enum NotificationChannelType { EMAIL, SMS, PUSH }
    enum NotificationPriority { LOW, NORMAL, HIGH, CRITICAL }

    static class Notification {
        private String notificationId;
        private String recipientId;
        private String message;
        private NotificationChannelType channel;
        private NotificationPriority priority;
    }

    static class UserNotificationPreference {
        private String userId;
        private Set<NotificationChannelType> enabledChannels;
    }

    interface NotificationChannel {
        void send(Notification notification);
    }

    interface NotificationProvider {
        ProviderResult deliver(Notification notification);
    }

    interface RetryPolicy {
        boolean shouldRetry(Exception exception, int attempt);
        Duration getRetryDelay(int attempt);
    }

    interface NotificationPreferenceService {
        boolean isEnabled(String userId, NotificationChannelType channel);
    }

    interface NotificationService {
        String send(Notification notification);
        void retry(String notificationId);
    }

    static class ProviderResult {
        private boolean successful;
        private String providerMessageId;
        private String errorMessage;
    }

    // ============================================================
    // 6. LOGGING FRAMEWORK
    // ============================================================

    enum LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

    static class LogEvent {
        private Instant timestamp;
        private LogLevel level;
        private String loggerName;
        private String message;
        private Throwable throwable;
    }

    interface Logger {
        void log(LogLevel level, String message);
        void log(LogLevel level, String message, Throwable throwable);
    }

    interface LogAppender {
        void append(LogEvent event);
    }

    interface LogFormatter {
        String format(LogEvent event);
    }

    interface LogFilter {
        boolean shouldLog(LogEvent event);
    }

    interface LoggingConfiguration {
        LogLevel getMinimumLevel();
        List<LogAppender> getAppenders();
        LogFormatter getFormatter();
    }

    // ============================================================
    // 7. MOVIE TICKET BOOKING SYSTEM
    // ============================================================

    enum SeatStatus { AVAILABLE, LOCKED, BOOKED }
    enum BookingStatus { INITIATED, CONFIRMED, EXPIRED, CANCELLED, FAILED }

    static class Movie {
        private String movieId;
        private String title;
        private Duration duration;
    }

    static class Theatre {
        private String theatreId;
        private List<Screen> screens;
    }

    static class Screen {
        private String screenId;
        private List<Seat> seats;
    }

    static class Seat {
        private String seatId;
        private String row;
        private int number;
    }

    static class Show {
        private String showId;
        private Movie movie;
        private Screen screen;
        private LocalDateTime startTime;
    }

    static class SeatLock {
        private String lockId;
        private String showId;
        private String seatId;
        private String userId;
        private Instant expiresAt;
    }

    static class Booking {
        private String bookingId;
        private String userId;
        private String showId;
        private List<Seat> seats;
        private BookingStatus status;
    }

    interface SeatLockService {
        Optional<SeatLock> lockSeats(String showId, List<String> seatIds, String userId, Duration lockDuration);
        void releaseLock(String lockId);
        boolean isLockValid(String lockId);
    }

    interface BookingService {
        Booking createBooking(String userId, String showId, List<String> seatIds);
        BookingStatus confirmBooking(String bookingId, String paymentReference);
        void cancelBooking(String bookingId);
    }

    interface SeatAvailabilityService {
        List<Seat> getAvailableSeats(String showId);
    }

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

    // ============================================================
    // 9. RATE LIMITER SERVICE
    // ============================================================

    enum RateLimitAlgorithm { FIXED_WINDOW, SLIDING_WINDOW, TOKEN_BUCKET, LEAKY_BUCKET }

    static class RateLimitRequest {
        private String clientId;
        private String resourceKey;
        private int requestedTokens;
    }

    static class RateLimitResult {
        private boolean allowed;
        private long remainingRequests;
        private Instant retryAfter;
    }

    static class RateLimitConfig {
        private int limit;
        private Duration window;
        private RateLimitAlgorithm algorithm;
    }

    interface RateLimiter {
        RateLimitResult allowRequest(RateLimitRequest request);
    }

    interface RateLimitAlgorithmStrategy {
        RateLimitResult evaluate(RateLimitRequest request, RateLimitConfig config);
    }

    interface RateLimitConfigProvider {
        RateLimitConfig getConfig(String clientId, String resourceKey);
    }

    interface RateLimitStateStore {
        long get(String key);
        void put(String key, long value, Duration ttl);
        boolean compareAndSet(String key, long expected, long updated);
    }

    // ============================================================
    // 10. JOB SCHEDULER / TASK EXECUTION FRAMEWORK
    // ============================================================

    enum JobStatus { SCHEDULED, QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }
    enum JobType { ONE_TIME, RECURRING }

    static class Job {
        private String jobId;
        private JobType type;
        private JobStatus status;
        private Instant scheduledAt;
        private int priority;
        private int maxRetries;
    }

    interface JobTask {
        void execute(JobExecutionContext context) throws Exception;
    }

    interface JobScheduler {
        String schedule(JobDefinition definition);
        boolean cancel(String jobId);
        Optional<Job> getJob(String jobId);
    }

    interface JobExecutionStrategy {
        void execute(Job job);
    }

    interface JobRetryPolicy {
        boolean shouldRetry(Job job, Exception exception);
        Duration getRetryDelay(Job job, int attempt);
    }

    interface JobStore {
        void save(Job job);
        Optional<Job> findById(String jobId);
        List<Job> findDueJobs(Instant now);
        void updateStatus(String jobId, JobStatus status);
    }

    interface JobQueue {
        void enqueue(Job job);
        Optional<Job> dequeue();
    }

    static class JobDefinition {
        private JobTask task;
        private Instant scheduledAt;
        private JobType type;
        private int priority;
        private int maxRetries;
    }

    static class JobExecutionContext {
        private String jobId;
        private int attempt;
        private Map<String, Object> attributes;
    }
}
