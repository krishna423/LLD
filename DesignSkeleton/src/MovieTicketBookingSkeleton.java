import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MovieTicketBookingSkeleton {

    // ============================================================
    // 7. MOVIE TICKET BOOKING SYSTEM
    // ============================================================

/**
 1.--------- Architectural thinking ------------------------------

         I'll first break the movie booking system into four major responsibilities:

         1. Movie and theatre catalog management
         2. Show and seat availability management
         3. Booking lifecycle management
         4. Payment and pricing management

         Then I'll identify the entities involved in each responsibility and define how they interact.

2. Identify the domain objects for each responsibility
         1. Catalog
            -- Movie, Theatre, Screen, City, Cinema

         2. Inventory - Show and seat availability
            -- Show, Seat, ShowSeat, SeatLock

         3. Booking lifecycle
            -- Booking, BookingService, BookingStatus

         4. Payment and pricing
            -- Payment, PricingStrategy, PaymentGateway, Refund



 // 3.--------- Identify the orchestration layer --------------------

     // Explain that a BookingService coordinates the booking workflow:
     // 1. Validate show and seat availability.
     // 2. Lock the selected seats temporarily.
     // 3. Calculate the booking amount.
     // 4. Initiate payment.
     // 5. Confirm booking after successful payment.
     // 6. Release the seat lock if payment fails or expires.
     //
     // Individual entities manage their own state and behavior,
     // while BookingService coordinates the overall booking lifecycle.



 You can introduce separate services if the requirements grow:
 - CatalogService – manages movies, theatres, screens and shows.
 - SeatInventoryService – manages seat availability and locking.
 - BookingService – coordinates booking and cancellation.
 - PaymentService – handles payments and refunds.


 // 4.--------- Identify extension points ---------------------------

      1. Seat allocation / locking
         - Temporary seat locking with an expiry time.
         - Different locking strategies.
         - Concurrent booking requests for the same seat.

      2. Pricing
         - Standard pricing.
         - Weekend / holiday pricing.
         - Dynamic pricing based on demand.
         - Different prices for different seat categories.

      3. Payment
         - Multiple payment gateways.
         - Payment retries.
         - Refunds and partial refunds.

      4. Show and screening types
         - 2D, 3D, IMAX, 4DX.
         - Different screen and seat categories.

      5. Booking lifecycle
         - Booking confirmation.
         - Cancellation.
         - Booking expiry.
         - Failed payment handling.

      6. Concurrency and scalability
         - Multiple users selecting the same seat simultaneously.
         - Atomic seat locking.
         - Distributed locking or database-level concurrency control.

**/


    enum SeatStatus { AVAILABLE, LOCKED, BOOKED }
    enum BookingStatus { INITIATED, CONFIRMED, EXPIRED, CANCELLED, FAILED }


    //Catalog management
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


    //Inventory Management
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

    interface SeatAvailabilityService {
        List<Seat> getAvailableSeats(String showId);
    }

    interface SeatLockService {
        Optional<SeatLock> lockSeats(String showId, List<String> seatIds, String userId, Duration lockDuration);
        void releaseLock(String lockId);
        boolean isLockValid(String lockId);
    }

    //Booking lifecycle
    static class Booking {
        private String bookingId;
        private String userId;
        private String showId;
        private List<Seat> seats;
        private BookingStatus status;
    }

    interface BookingService {
        Booking createBooking(String userId, String showId, List<String> seatIds);
        BookingStatus confirmBooking(String bookingId, String paymentReference);
        void cancelBooking(String bookingId);
    }


}
