import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class BaseSkeleton {

    // ============================================================
    // 1. GENERAL BOOKING / ORDER MANAGEMENT SYSTEM
    // ============================================================

    // 1.--------- Architectural Thinking ------------------------
    //
    // I'll divide the system into five major responsibilities:
    //
    // 1. Catalog management
    // 2. Availability and inventory management
    // 3. Booking / Order lifecycle management
    // 4. Payment management
    // 5. Refund and cancellation management
    //
    // Individual services own their respective responsibilities.
    // BookingService coordinates the customer booking workflow.


    // 2.--------- Identify Domain Objects -----------------------
    //
    // 1. Catalog       -- Product, Location, Resource
    // 2. Availability  -- Inventory, Slot, Capacity, Reservation
    // 3. Booking       -- Booking, BookingItem, BookingStatus
    // 4. Payment       -- Payment, PaymentStatus
    // 5. Refund        -- Refund, RefundStatus


    // 3.--------- Identify Orchestration Layer ------------------
    //
    // BookingService coordinates:
    // - Catalog validation
    // - Availability check
    // - Resource reservation
    // - Booking creation
    // - Payment collection
    // - Booking confirmation
    //
    // On payment failure, release the reservation.


    // 4.--------- Identify What May Change ----------------------
    //                 Open-Closed Principle
    //
    // - Catalog types may change.
    // - Availability can be inventory-based or capacity-based.
    // - Booking rules may change.
    // - Pricing can be fixed or dynamic.
    // - Payment gateway can change.
    // - Cancellation and refund policies can change.


    // ============================================================
    // 1. CATALOG MANAGEMENT
    // ============================================================

    static class CatalogItem {
        private String id;
        private String name;
        private String description;
        private boolean active;
    }

    interface CatalogService {

        void addItem(CatalogItem item);

        void updateItem(CatalogItem item);

        Optional<CatalogItem> getItem(String itemId);

        List<CatalogItem> searchItems(String criteria);
    }


    // ============================================================
    // 2. AVAILABILITY / INVENTORY MANAGEMENT
    // ============================================================

    static class Resource {
        private String resourceId;
        private String name;
    }

    static class AvailabilitySlot {
        private String slotId;
        private Instant startTime;
        private Instant endTime;
        private int capacity;
        private int availableCapacity;
    }

    static class Reservation {
        private String reservationId;
        private String resourceId;
        private Instant expiryTime;
    }

    interface AvailabilityService {

        // Check whether requested resource is available.
        boolean checkAvailability(String resourceId, int quantity);

        // Atomically reserve resource/capacity.
        Optional<Reservation> reserve(String resourceId, int quantity);

        // Release reserved capacity.
        void release(String reservationId);
    }


    // ============================================================
    // 3. BOOKING / ORDER MANAGEMENT
    // ============================================================

    enum BookingStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
        EXPIRED
    }

    static class Booking {

        private String bookingId;
        private String customerId;
        private List<CatalogItem> items;
        private Reservation reservation;
        private BigDecimal totalAmount;
        private BookingStatus status;
        private Instant createdAt;
    }

    interface BookingService {

        Booking createBooking(String customerId, List<CatalogItem> items);
        void confirmBooking(String bookingId);
        void cancelBooking(String bookingId);
        Optional<Booking> getBooking(String bookingId);
    }


    // ============================================================
    // 4. PAYMENT MANAGEMENT
    // ============================================================

    enum PaymentStatus {
        INITIATED,
        SUCCESS,
        FAILED,
        REFUNDED
    }

    static class Payment {

        private String paymentId;
        private String bookingId;
        private BigDecimal amount;
        private PaymentStatus status;
    }

    interface PaymentGateway {

        Payment processPayment(String bookingId, BigDecimal amount);
    }

    interface PaymentService {

        Payment makePayment(String bookingId, BigDecimal amount);
        PaymentStatus getPaymentStatus(String paymentId);
    }


    // ============================================================
    // 5. REFUND / CANCELLATION MANAGEMENT
    // ============================================================

    static class Refund {

        private String refundId;
        private String paymentId;
        private BigDecimal amount;
    }

    interface RefundPolicy {

        BigDecimal calculateRefund(Booking booking);
    }

    interface RefundService {

        Refund initiateRefund(String paymentId, BigDecimal amount);
    }


    // ============================================================
    // 6. PRICING MANAGEMENT - OPTIONAL EXTENSION
    // ============================================================

    interface PricingStrategy {

        BigDecimal calculatePrice(List<CatalogItem> items);
    }


    // ============================================================
    // 7. BOOKING ORCHESTRATION
    // ============================================================

    interface BookingOrchestrator {

        Booking book(String customerId, List<CatalogItem> items);

        void cancel(String bookingId);
    }

    static class BookingOrchestratorImpl implements BookingOrchestrator {

        private CatalogService catalogService;
        private AvailabilityService availabilityService;
        private BookingService bookingService;
        private PaymentService paymentService;
        private RefundService refundService;

        @Override
        public Booking book(String customerId, List<CatalogItem> items) {

            // 1. Validate catalog items.

            // 2. Check availability.

            // 3. Reserve inventory/capacity.

            // 4. Create PENDING booking.

            // 5. Calculate total amount.

            // 6. Initiate payment.

            // 7. If payment succeeds:
            //    Confirm booking.

            // 8. If payment fails:
            //    Cancel booking and release reservation.

            return null;
        }

        @Override
        public void cancel(String bookingId) {

            // 1. Fetch booking.

            // 2. Validate cancellation eligibility.

            // 3. Cancel booking.

            // 4. Release reserved inventory.

            // 5. Calculate refund based on RefundPolicy.

            // 6. Initiate refund if applicable.
        }
    }
}
