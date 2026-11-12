import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RateLimiterSkeleton {

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
}
