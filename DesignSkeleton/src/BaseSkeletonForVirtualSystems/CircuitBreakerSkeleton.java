package BaseSkeletonForVirtualSystems;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;

public class CircuitBreakerSkeleton {



    enum CircuitBreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    interface CircuitBreaker {
        <T> T execute(Callable<T> operation);
        CircuitBreakerState getState();
        boolean allowRequest(); // allow depends on state

        // IRequestStatusTracker requestStatusTracker
        // CircuitBrakerState
    }

     interface IRequestStatusTracker {
        void recordSuccess(); // update queue with (time,Success)
        void recordFailure(); // update queue with (time,failure)
        void removeExpiredRequests();
        double getFailureRate();

        // Queue<RequestRecord> to hold records
    }

    public class RequestStatusTracker implements IRequestStatusTracker {

        private final Queue<RequestRecord> requests = new LinkedList<>(); // 1sec-noOfCounts
        private final long windowSizeMs;

        public RequestStatusTracker(long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
        }

        public synchronized void recordSuccess() {
            requests.add(new RequestRecord(System.currentTimeMillis(), true));
        }

        public synchronized void recordFailure() {
            requests.add(new RequestRecord(System.currentTimeMillis(), false));
        }

        public void removeExpiredRequests() {
            long currentTime = System.currentTimeMillis();
            while (!requests.isEmpty()) {
                RequestRecord record = requests.peek();
                if (currentTime - record.timestamp > windowSizeMs) {
                    requests.poll();
                } else {
                    break;
                }
            }
        }

        public synchronized double getFailureRate() {
            removeExpiredRequests();
            if (requests.isEmpty()) {
                return 0;
            }
            long failures = requests.stream()
                    .filter(r -> !r.success)
                    .count();

            return (failures * 100.0) / requests.size();
        }
    }


    class CircuitBreakerImpl implements CircuitBreaker {

        private final CircuitBreakerConfig config;
        private final RequestStatusTracker failureTracker;

        private CircuitBreakerState state;

        private long openTimestamp;

        // HALF_OPEN concurrency tracking
        private int halfOpenInFlight;

        public CircuitBreakerImpl(CircuitBreakerConfig config) {
            this.config = config;
            this.failureTracker = new RequestStatusTracker(config.halfOpenMaxCalls);
            this.state = CircuitBreakerState.CLOSED;
        }

        @Override
        public <T> T execute(Callable<T> operation) {
            // 1. Check whether request is allowed
            // 2. Execute downstream operation
            // 3. Record success or failure  -- queue will be updated automatically
            return null;
        }

        @Override
        public boolean allowRequest() {
            //Option1: getState(), using schedular --updateState

            //Option2: using realtime check and transitions state
            // TODO: CLOSED -> Allow all requests
            // TODO: OPEN circuit for 10 mins ->  mark HalfOpen
            // TODO: HALF_OPEN -> Allow limited trial requests
            // one limit reached - check failure%-- mark state to OPEN/CLose accordingly

            //State can be a enum which have allowRequest function and all with have separate Implementation
            return false;
        }

        @Override
        public synchronized CircuitBreakerState getState() {
            return state;
        }

    }



// ============================================================
// 2. CONFIGURATION
// ============================================================

    class CircuitBreakerConfig {
        int failureThreshold;
        long openDurationInSec;
        int halfOpenMaxCalls;
        int halfOpenSuccessThreshold;
    }

    class RequestRecord {
        long timestamp;
        boolean success;

        RequestRecord(long timestamp, boolean success) {
            this.timestamp = timestamp;
            this.success = success;
        }
    }

    class PaymentService {

        public String processPayment() {

            // Simulate downstream payment API
            return "Payment processed successfully";
        }
    }

// ============================================================
// 8. APPLICATION ENTRY POINT
// ============================================================

    public class CircuitBreakerDemo {

        public static void main(String[] args) {

            CircuitBreakerSkeleton skeleton = new CircuitBreakerSkeleton();
            CircuitBreakerSkeleton.CircuitBreakerConfig config =
                    skeleton.new CircuitBreakerConfig();

            CircuitBreakerSkeleton.CircuitBreaker circuitBreaker =
                    skeleton.new CircuitBreakerImpl(config);

            CircuitBreakerSkeleton.PaymentService paymentService = skeleton.new PaymentService();

            String response = circuitBreaker.execute(
                    paymentService::processPayment
            );

            System.out.println(response);
        }
    }

}
