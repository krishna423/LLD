package BaseLearning;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ParkingLotSkeleton {

    // ============================================================
    // 1. PARKING LOT MANAGEMENT SYSTEM
    // ============================================================


//1.---------architectural thinking------------------------------
    // I'll first break the parking lot into three major responsibilities:
    // 1.Parking spot allocation/catelog,--Non-functional but ready to use parking lot
    // 2.Parking lifecycle management,- funcational at real time
    // 3.Payment calculation.
    // Then I'll identify the entities involved in each responsibility and define how they interact


//2. Identify the domain objects for each responsibility
    //1.Parking allocation/catelog	-- ParkingLot, ParkingFloor, ParkingSpot, Vehicle
    //2.Parking lifecycle	--  EntryGate, Ticket, ExitGate
    //3.Fee management	-- Payment, PricingStrategy

//3. Identify the orchestration layer
    //Explain that a ParkingService coordinates the parking and unparking use cases,
    // while individual objects manage their own state and behavior.

//4. Identify what may change -- Extension open-closed principle
    //This is where you can demonstrate senior-level thinking:
    //- Parking allocation-- may change from nearest spot to nearest-floor or optimized allocation.
    //- Pricing-- may change from hourly to vehicle-based or dynamic pricing.
    //- New vehicle types--- and spot types may be introduced.
    //- Multiple entry gates--- may allocate spots concurrently.
    // CancellationPolicy can change

    //1st Parking catalog management
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

    enum VehicleType { BIKE, CAR, TRUCK }
    enum SpotType { BIKE, COMPACT, LARGE }


    //2. Parking lifeCycle management
    static class ParkingTicket {
        private String ticketId;
        private Vehicle vehicle;
        private ParkingSpot spot;
        private Instant entryTime;
    }

    interface SpotAllocationStrategy {
        Optional<ParkingSpot> findSpot(Vehicle vehicle, List<ParkingFloor> floors);
    }
    //Nearest parking or Round-robin floor wise or near to exit Gate

    interface ParkingFeeStrategy {
        double calculateFee(ParkingTicket ticket, Instant exitTime); //VehicleType
    }
    //Hourly basis or day basis


    static class EntryGate {
        private String gateId;
    }

    static class ExitGate {
        private String gateId;
    }

    interface ParkingService {
        ParkingTicket parkVehicle(Vehicle vehicle,EntryGate entryGate);
        //findSpot using given strategy
        //bookTicket
        //Marked ParkingSlot as booked

        PaymentReceipt unparkVehicle(ParkingTicket parkingTicket,ExitGate exitGate);
        //calculate time/duration
        // use FeeStrategy to calculateAmount
        // call for payment
        // release the parkingLot
    }

    static class PaymentReceipt {
        private String invoiceNumber;
    }
    interface ParkingPaymentService {
        boolean collectPayment(String ticketId, double amount);
    }

}
