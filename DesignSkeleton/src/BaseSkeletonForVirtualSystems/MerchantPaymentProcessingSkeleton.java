package BaseSkeletonForVirtualSystems;

import java.util.Optional;

public class MerchantPaymentProcessingSkeleton {

//Flow client--> PaymentService -- PaymentProvider---DB

    enum PaymentStatus { CREATED, INITIATED, SUCCESS, FAILED, REFUNDED }
    enum PaymentMethodType { UPI, CARD, NET_BANKING }


    interface IPaymentProvider {
        PaymentResult charge(Payment payment);
        RefundResult refund(Payment payment, double amount);
        PaymentResult getPaymentStatus(String providerTransactionId);
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

    static class Payment {
        private String paymentId;
        private String orderId;
        private double amount;
        private PaymentStatus status;
        private PaymentMethodType methodType;
    }

    //Client initiated payment request
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

    class RazorpayProvider implements IPaymentProvider {
        @Override
        public PaymentResult charge(Payment payment) {
            return null;
        }

        @Override
        public RefundResult refund(Payment payment, double amount) {
            return null;
        }

        @Override
        public PaymentResult getPaymentStatus(String providerTransactionId) {
            return null;
        }
        // Razorpay-specific implementation
    }
}
