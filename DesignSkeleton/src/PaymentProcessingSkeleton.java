import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PaymentProcessingSkeleton {

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
}
