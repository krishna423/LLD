package BaseSkeletonForVirtualSystems;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RazorPayPGSkeleton {

    // ==================================================
    // 1. ENUMS
    // ==================================================

    enum PaymentMethod {
        UPI, CARD, NET_BANKING
    }

    enum PaymentStatus {
        CREATED, PROCESSING, REQUIRES_ACTION, AUTHORIZED, CAPTURED, FAILED, UNKNOWN
    }

    enum RefundStatus {
        CREATED, PROCESSING, SUCCESS, FAILED
    }
    // ==================================================
    // 3. PAYMENT PROCESSOR ABSTRACTION
    // ==================================================

    interface IPaymentProcessor {
        PaymentResult processPayment(Payment payment);
        PaymentResult checkPaymentStatus(String providerReferenceId);
        RefundStatus refund(Payment payment, BigDecimal amount);
    }

    // ==================================================
    // 5. PAYMENT PROCESSOR FACTORY
    // ==================================================

    interface IPaymentProcessorFactory {
        IPaymentProcessor getProcessor(PaymentMethod method);
    }


    // ==================================================
    // 9. PAYMENT STATE MANAGER
    // ==================================================

    interface IPaymentStateManager {
        boolean canTransition(PaymentStatus current, PaymentStatus next);
        void transition(Payment payment, PaymentStatus next);
    }

    // ==================================================
    // 6. MERCHANT SERVICE
    // ==================================================

    interface IMerchantService {
        boolean isValidMerchant(String merchantId);
    }

    // ==================================================
    // 7. PAYMENT REPOSITORY
    // ==================================================

    interface IPaymentRepository {
        void save(Payment payment);
        Payment findById(String paymentId);
        void update(Payment payment);
    }

    // ==================================================
    // 8. IDEMPOTENCY MANAGER
    // ==================================================

    interface IIdempotencyManager {
        Payment getExistingPayment(String merchantId, String idempotencyKey);
        void register(String merchantId, String idempotencyKey, Payment payment);
    }



    static class PaymentStateManager
            implements IPaymentStateManager {

        private final Map<PaymentStatus, Set<PaymentStatus>> transitions = new EnumMap<>(PaymentStatus.class);

        PaymentStateManager() {

            transitions.put(PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PROCESSING));

            transitions.put(PaymentStatus.PROCESSING,
                    EnumSet.of(
                            PaymentStatus.CAPTURED,
                            PaymentStatus.AUTHORIZED,
                            PaymentStatus.REQUIRES_ACTION,
                            PaymentStatus.FAILED,
                            PaymentStatus.UNKNOWN));

            transitions.put(
                    PaymentStatus.UNKNOWN,
                    EnumSet.of(
                            PaymentStatus.CAPTURED,
                            PaymentStatus.FAILED,
                            PaymentStatus.PROCESSING));

            transitions.put(
                    PaymentStatus.AUTHORIZED,
                    EnumSet.of(
                            PaymentStatus.CAPTURED,
                            PaymentStatus.FAILED));
        }

        @Override
        public boolean canTransition(PaymentStatus current, PaymentStatus next) {
            return transitions.getOrDefault(current, Collections.emptySet()).contains(next);
        }

        @Override
        public void transition(Payment payment, PaymentStatus next) {

            if (!canTransition(payment.status, next)) {
                throw new IllegalStateException("Invalid transition: " + payment.status + " -> " + next);
            }
            payment.status = next;
            payment.updatedAt = LocalDateTime.now();
        }
    }

    // ==================================================
    // 10. LEDGER SERVICE
    // ==================================================

    interface ILedgerService {
        void recordPayment(Payment payment);
        void recordRefund(Refund refund);
    }

    static class InMemoryLedgerService implements ILedgerService {

        private final List<String> entries = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void recordPayment(Payment payment) {
            entries.add("PAYMENT: " + payment.paymentId + " | Amount: " + payment.amount);
        }

        @Override
        public void recordRefund(Refund refund) {
            entries.add("REFUND: " + refund.refundId + " | Amount: " + refund.amount);
        }

        public void printEntries() {
            synchronized (entries) {
                entries.forEach(System.out::println);
            }
        }
    }

    // ==================================================
    // 11. REFUND REPOSITORY
    // ==================================================

    interface IRefundRepository {
        void save(Refund refund);
        Refund findById(String refundId);
        Refund findByIdempotencyKey(String paymentId, String key);
    }

    static class InMemoryRefundRepository implements IRefundRepository {

        private final Map<String, Refund> refunds = new ConcurrentHashMap<>();

        @Override
        public void save(Refund refund) {
            refunds.put(refund.refundId, refund);
        }

        @Override
        public Refund findById(String refundId) {
            return refunds.get(refundId);
        }

        @Override
        public Refund findByIdempotencyKey(String paymentId, String key) {

            return refunds.values().stream()
                    .filter(r -> r.paymentId.equals(paymentId))
                    .filter(r -> r.idempotencyKey.equals(key))
                    .findFirst()
                    .orElse(null);
        }
    }

    // ==================================================
    // 12. PAYMENT ORCHESTRATOR
    // ==================================================

    static class PaymentOrchestrator {

        private final IPaymentProcessorFactory processorFactory;
        private final IPaymentRepository paymentRepository;
        private final IPaymentStateManager stateManager;
        private final ILedgerService ledgerService;

        PaymentOrchestrator(
                IPaymentProcessorFactory processorFactory,
                IPaymentRepository paymentRepository,
                IPaymentStateManager stateManager,
                ILedgerService ledgerService) {

            this.processorFactory = processorFactory;
            this.paymentRepository = paymentRepository;
            this.stateManager = stateManager;
            this.ledgerService = ledgerService;
        }

        PaymentResponse process(Payment payment) {

            stateManager.transition(payment, PaymentStatus.PROCESSING);
            paymentRepository.update(payment);

            IPaymentProcessor processor = processorFactory.getProcessor(payment.paymentMethod);

            try {
                PaymentResult result = processor.processPayment(payment);
                payment.providerReferenceId = result.providerReferenceId;
                stateManager.transition(payment, result.status);
                paymentRepository.update(payment);

                if (result.status == PaymentStatus.CAPTURED) {
                    ledgerService.recordPayment(payment);
                }

                return new PaymentResponse(payment.paymentId, payment.status, result.message);

            } catch (Exception exception) {

                // A timeout does not prove that payment failed.
                stateManager.transition(payment, PaymentStatus.UNKNOWN);
                paymentRepository.update(payment);

                return new PaymentResponse(payment.paymentId, payment.status, "Payment outcome unknown. Reconciliation required");
            }
        }

        PaymentResponse getPaymentStatus(String paymentId) {

            Payment payment = paymentRepository.findById(paymentId);

            if (payment == null) {throw new IllegalArgumentException("Payment not found");}

            if (payment.status == PaymentStatus.UNKNOWN && payment.providerReferenceId != null) {

                IPaymentProcessor processor = processorFactory.getProcessor(payment.paymentMethod);
                PaymentResult result = processor.checkPaymentStatus(payment.providerReferenceId);

                if (result.status == PaymentStatus.CAPTURED || result.status == PaymentStatus.FAILED) {

                    stateManager.transition(payment, result.status);
                    paymentRepository.update(payment);

                    if (result.status == PaymentStatus.CAPTURED) {
                        ledgerService.recordPayment(payment);
                    }
                }
            }

            return new PaymentResponse(payment.paymentId, payment.status, "Current payment status");
        }
    }

    // ==================================================
    // 13. REFUND SERVICE
    // ==================================================

    static class RefundService {

        private final IPaymentRepository paymentRepository;
        private final IRefundRepository refundRepository;
        private final IPaymentProcessorFactory processorFactory;
        private final ILedgerService ledgerService;

        RefundService(
                IPaymentRepository paymentRepository,
                IRefundRepository refundRepository,
                IPaymentProcessorFactory processorFactory,
                ILedgerService ledgerService) {

            this.paymentRepository = paymentRepository;
            this.refundRepository = refundRepository;
            this.processorFactory = processorFactory;
            this.ledgerService = ledgerService;
        }

        public synchronized Refund initiateRefund(
                RefundRequest request) {

            Refund existing =
                    refundRepository.findByIdempotencyKey(
                            request.paymentId,
                            request.idempotencyKey);

            if (existing != null) {
                return existing;
            }

            Payment payment =
                    paymentRepository.findById(request.paymentId);

            if (payment == null) {
                throw new IllegalArgumentException(
                        "Payment not found");
            }

            if (payment.status != PaymentStatus.CAPTURED) {
                throw new IllegalStateException(
                        "Only captured payments can be refunded");
            }

            if (request.amount == null ||
                    request.amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Refund amount must be positive");
            }

            BigDecimal alreadyRefunded = new BigDecimal(121);
                    //refundRepository.findSuccessfulRefundsAmount(
                           // request.paymentId);

            if (alreadyRefunded.add(request.amount)
                    .compareTo(payment.amount) > 0) {
                throw new IllegalArgumentException(
                        "Refund exceeds captured amount");
            }

            Refund refund = new Refund(request);
            refund.status = RefundStatus.PROCESSING;
            refundRepository.save(refund);

            IPaymentProcessor processor =
                    processorFactory.getProcessor(
                            payment.paymentMethod);

            RefundStatus result =
                    processor.refund(payment, request.amount);

            refund.status = result;

            if (result == RefundStatus.SUCCESS) {
                ledgerService.recordRefund(refund);
            }

            return refund;
        }
    }

    // ==================================================
    // 14. PAYMENT SERVICE - ENTRY POINT
    // ==================================================

    static class PaymentService {

        private final IMerchantService merchantService;
        private final IPaymentRepository paymentRepository;
        private final IIdempotencyManager idempotencyManager;
        private final PaymentOrchestrator orchestrator;

        PaymentService(
                IMerchantService merchantService,
                IPaymentRepository paymentRepository,
                IIdempotencyManager idempotencyManager,
                PaymentOrchestrator orchestrator) {

            this.merchantService = merchantService;
            this.paymentRepository = paymentRepository;
            this.idempotencyManager = idempotencyManager;
            this.orchestrator = orchestrator;
        }

        public synchronized PaymentResponse createPayment(PaymentRequest request) {

            // 1. Validate merchant
            if (!merchantService.isValidMerchant(request.merchantId)) {
                throw new IllegalArgumentException("Invalid merchant");
            }

            // 2. Validate request
            if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid payment amount");
            }

            if (request.idempotencyKey == null || request.idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("Idempotency key is required");
            }

            if (request.paymentMethod == null) {throw new IllegalArgumentException("Payment method is required");
            }

            // 3. Idempotency check
            Payment existing = idempotencyManager.getExistingPayment(request.merchantId, request.idempotencyKey);

            if (existing != null) {
                return new PaymentResponse(existing.paymentId, existing.status, "Existing payment returned");
            }

            // 4. Create payment
            Payment payment = new Payment(request);
            paymentRepository.save(payment);

            // 5. Register idempotency key
            idempotencyManager.register(request.merchantId, request.idempotencyKey, payment);

            // 6. Process payment
            return orchestrator.process(payment);
        }

        public PaymentResponse getPaymentStatus(
                String paymentId) {

            return orchestrator.getPaymentStatus(paymentId);
        }
    }


    // ==================================================
    // 4. CONCRETE PAYMENT PROCESSORS
    // ==================================================

    static class UpiPaymentProcessor implements IPaymentProcessor {

        @Override
        public PaymentResult processPayment(Payment payment) {

            System.out.println("Processing UPI payment...");

            // Simulated UPI network call
            return new PaymentResult(PaymentStatus.CAPTURED, "UPI-" + UUID.randomUUID(), "UPI payment successful");
        }

        @Override
        public PaymentResult checkPaymentStatus(String referenceId) {
            return new PaymentResult(PaymentStatus.CAPTURED, referenceId, "UPI payment successful");
        }

        @Override
        public RefundStatus refund(Payment payment, BigDecimal amount) {
            System.out.println("Processing UPI refund...");
            return RefundStatus.SUCCESS;
        }
    }

    static class CardPaymentProcessor
            implements IPaymentProcessor {

        @Override
        public PaymentResult processPayment(Payment payment) {
            System.out.println("Processing Card payment...");
            return new PaymentResult(PaymentStatus.CAPTURED, "CARD-" + UUID.randomUUID(), "Card payment successful");
        }

        @Override
        public PaymentResult checkPaymentStatus(String referenceId) {
            return new PaymentResult(PaymentStatus.CAPTURED, referenceId, "Card payment successful");
        }

        @Override
        public RefundStatus refund(Payment payment, BigDecimal amount) {
            System.out.println("Processing Card refund...");
            return RefundStatus.SUCCESS;
        }
    }

    static class NetBankingProcessor implements IPaymentProcessor {

        @Override
        public PaymentResult processPayment(Payment payment) {
            System.out.println("Processing Net Banking payment...");

            return new PaymentResult(PaymentStatus.CAPTURED, "NB-" + UUID.randomUUID(), "Net banking payment successful");
        }

        @Override
        public PaymentResult checkPaymentStatus(String referenceId) {
            return new PaymentResult(PaymentStatus.CAPTURED, referenceId, "Net banking payment successful");
        }

        @Override
        public RefundStatus refund(Payment payment, BigDecimal amount) {
            System.out.println("Processing Net Banking refund...");
            return RefundStatus.SUCCESS;
        }
    }

    static class PaymentProcessorFactory implements IPaymentProcessorFactory {

        private final Map<PaymentMethod, IPaymentProcessor> processors = new EnumMap<>(PaymentMethod.class);

        PaymentProcessorFactory() {
            processors.put(PaymentMethod.UPI, new UpiPaymentProcessor());
            processors.put(PaymentMethod.CARD, new CardPaymentProcessor());
            processors.put(PaymentMethod.NET_BANKING, new NetBankingProcessor());
        }

        @Override
        public IPaymentProcessor getProcessor(PaymentMethod method) {

            IPaymentProcessor processor = processors.get(method);
            if (processor == null) {
                throw new IllegalArgumentException("Unsupported payment method");
            }

            return processor;
        }
    }

    static class MerchantService implements IMerchantService {

        private final Set<String> merchants = ConcurrentHashMap.newKeySet();

        MerchantService() {
            merchants.add("MERCHANT_101");
            merchants.add("MERCHANT_102");
        }

        @Override
        public boolean isValidMerchant(String merchantId) {
            return merchants.contains(merchantId);
        }
    }

    static class InMemoryPaymentRepository implements IPaymentRepository {

        private final Map<String, Payment> payments = new ConcurrentHashMap<>();

        @Override
        public void save(Payment payment) {
            payments.put(payment.paymentId, payment);
        }

        @Override
        public Payment findById(String paymentId) {
            return payments.get(paymentId);
        }

        @Override
        public void update(Payment payment) {
            payments.put(payment.paymentId, payment);
        }
    }

    // ==================================================
    // 8. IDEMPOTENCY MANAGER
    // ==================================================

    static class InMemoryIdempotencyManager implements IIdempotencyManager {

        private final Map<String, Payment> requests = new ConcurrentHashMap<>();

        private String getKey(String merchantId, String idempotencyKey) {
            return merchantId + ":" + idempotencyKey;
        }

        @Override
        public Payment getExistingPayment(String merchantId, String idempotencyKey) {
            return requests.get(getKey(merchantId, idempotencyKey));
        }

        @Override
        public void register(String merchantId, String idempotencyKey, Payment payment) {

            requests.put(getKey(merchantId, idempotencyKey), payment);
        }
    }



    // ==================================================
    // 2. DOMAIN MODELS
    // ==================================================

    static class PaymentRequest {
        String merchantId;
        String orderId;
        String idempotencyKey;
        BigDecimal amount;
        String currency;
        PaymentMethod paymentMethod;

        PaymentRequest(
                String merchantId,
                String orderId,
                String idempotencyKey,
                BigDecimal amount,
                String currency,
                PaymentMethod paymentMethod) {

            this.merchantId = merchantId;
            this.orderId = orderId;
            this.idempotencyKey = idempotencyKey;
            this.amount = amount;
            this.currency = currency;
            this.paymentMethod = paymentMethod;
        }
    }

    static class Payment {
        String paymentId;
        String merchantId;
        String orderId;
        String idempotencyKey;

        BigDecimal amount;
        String currency;

        PaymentMethod paymentMethod;
        PaymentStatus status;

        String providerReferenceId;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

        Payment(PaymentRequest request) {
            this.paymentId = UUID.randomUUID().toString();
            this.merchantId = request.merchantId;
            this.orderId = request.orderId;
            this.idempotencyKey = request.idempotencyKey;
            this.amount = request.amount;
            this.currency = request.currency;
            this.paymentMethod = request.paymentMethod;
            this.status = PaymentStatus.CREATED;
            this.createdAt = LocalDateTime.now();
            this.updatedAt = this.createdAt;
        }
    }

    static class PaymentResult {
        PaymentStatus status;
        String providerReferenceId;
        String message;

        PaymentResult(
                PaymentStatus status,
                String providerReferenceId,
                String message) {
            this.status = status;
            this.providerReferenceId = providerReferenceId;
            this.message = message;
        }
    }

    static class PaymentResponse {
        String paymentId;
        PaymentStatus status;
        String message;

        PaymentResponse(
                String paymentId,
                PaymentStatus status,
                String message) {
            this.paymentId = paymentId;
            this.status = status;
            this.message = message;
        }

        @Override
        public String toString() {
            return "PaymentResponse{" +
                    "paymentId='" + paymentId + '\'' +
                    ", status=" + status +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    static class RefundRequest {
        String paymentId;
        String idempotencyKey;
        BigDecimal amount;

        RefundRequest(
                String paymentId,
                String idempotencyKey,
                BigDecimal amount) {
            this.paymentId = paymentId;
            this.idempotencyKey = idempotencyKey;
            this.amount = amount;
        }
    }

    static class Refund {
        String refundId;
        String paymentId;
        String idempotencyKey;
        BigDecimal amount;
        RefundStatus status;

        Refund(RefundRequest request) {
            this.refundId = UUID.randomUUID().toString();
            this.paymentId = request.paymentId;
            this.idempotencyKey = request.idempotencyKey;
            this.amount = request.amount;
            this.status = RefundStatus.CREATED;
        }
    }






    // ==================================================
    // 15. APPLICATION WIRING
    // ==================================================

    public static void main(String[] args) {

        // Repositories
        IPaymentRepository paymentRepository =
                new InMemoryPaymentRepository();

        IRefundRepository refundRepository =
                new InMemoryRefundRepository();

        IIdempotencyManager idempotencyManager =
                new InMemoryIdempotencyManager();

        // Services
        IMerchantService merchantService =
                new MerchantService();

        IPaymentProcessorFactory processorFactory =
                new PaymentProcessorFactory();

        IPaymentStateManager stateManager =
                new PaymentStateManager();

        ILedgerService ledgerService =
                new InMemoryLedgerService();

        // Orchestrator
        PaymentOrchestrator orchestrator =
                new PaymentOrchestrator(
                        processorFactory,
                        paymentRepository,
                        stateManager,
                        ledgerService);

        // Payment service
        PaymentService paymentService =
                new PaymentService(
                        merchantService,
                        paymentRepository,
                        idempotencyManager,
                        orchestrator);

        // Refund service
        RefundService refundService =
                new RefundService(
                        paymentRepository,
                        refundRepository,
                        processorFactory,
                        ledgerService);

        // ==============================================
        // TEST 1: UPI PAYMENT
        // ==============================================

        PaymentRequest request = new PaymentRequest(
                "MERCHANT_101",
                "ORDER_1001",
                "IDEMPOTENCY_001",
                new BigDecimal("1000"),
                "INR",
                PaymentMethod.UPI
        );

        PaymentResponse response =
                paymentService.createPayment(request);

        System.out.println("\nPayment response:");
        System.out.println(response);

        // ==============================================
        // TEST 2: DUPLICATE REQUEST
        // ==============================================

        PaymentResponse duplicateResponse =
                paymentService.createPayment(request);

        System.out.println("\nDuplicate request:");
        System.out.println(duplicateResponse);

        // ==============================================
        // TEST 3: CHECK PAYMENT STATUS
        // ==============================================

        PaymentResponse statusResponse =
                paymentService.getPaymentStatus(
                        response.paymentId);

        System.out.println("\nPayment status:");
        System.out.println(statusResponse);

        // ==============================================
        // TEST 4: REFUND
        // ==============================================

        RefundRequest refundRequest = new RefundRequest(
                response.paymentId,
                "REFUND_KEY_001",
                new BigDecimal("300")
        );

        Refund refund =
                refundService.initiateRefund(refundRequest);

        System.out.println("\nRefund response:");
        System.out.println(
                refund.refundId + " | " +
                        refund.status + " | " +
                        refund.amount);

        // ==============================================
        // TEST 5: LEDGER
        // ==============================================

        System.out.println("\nLedger entries:");
        ((InMemoryLedgerService) ledgerService).printEntries();
    }
}

