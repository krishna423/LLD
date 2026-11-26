package BaseSkeletonForVirtualSystems;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterSkeleton {


    /**
     * Config
     * 10 sec , allow only 5 requests
     * Fixed- {0-9 counter}, {10-19, counter}, update counter
     * Sliding - {currentTime-10sec, currentTime}: queue all requests
     * TokenBucket --> TokenCount=5 --- each request reduce the count if 0 then rejected
     * LeakyBucket - { park requests in queue} and release at fixed rate
     * client bases, or Ip bases ??
     *
     */

    // ============================================================
    // Flow ---> client --> rateLimiter ---> request (Pass or rejected)
    // ============================================================


    interface IRateLimiterService {
        Response validateRequest(Request request);
        //IRateLimitingStrategy
    }


    enum RateLimitingAlgorithm {
        FIXED_WINDOW, SLIDING_WINDOW, LEAKY_BUCKET, TOKEN_BUCKET
    }


    interface IRateLimitingStrategy {
        Response evaluateRequest(Request request);
    }


    private static class Request {
        String clientName;
        String requestId;
    }

    private static class Response {
        boolean allowed;
    }


    static class RateLimitingConfiguration {
        int maxAllowedRequestCount;
        int windowTime;
    }

    static class FixedWindowIRateLimitingStrategy implements IRateLimitingStrategy {

        RateLimitingConfiguration rateLimitingConfiguration;
        private final Map<String, FixedWindow> clientWindows = new ConcurrentHashMap<>();

        public FixedWindowIRateLimitingStrategy(RateLimitingConfiguration rateLimitingConfiguration) {
            this.rateLimitingConfiguration = rateLimitingConfiguration;
        }

        @Override
        public synchronized Response evaluateRequest(Request request) {

            Response response = new Response();
            //No window created or window expired
            if (!clientWindows.containsKey(request.clientName)
                    || clientWindows.get(request.clientName).windowEndTime.isBefore(LocalDateTime.now())) {

                clientWindows.put(request.clientName, new FixedWindow(LocalDateTime.now(),
                        LocalDateTime.now().plus(rateLimitingConfiguration.windowTime, ChronoUnit.MINUTES), 0));
            }

            FixedWindow fixedWindow = clientWindows.get(request.clientName);

            if (fixedWindow.count < rateLimitingConfiguration.maxAllowedRequestCount) {
                fixedWindow.count++;
                response.allowed = true;
            } else {
                response.allowed = false;
            }
            return response;
        }
    }

    static class FixedWindow {
        LocalDateTime windowStartTime;
        LocalDateTime windowEndTime;
        int count;

        public FixedWindow(LocalDateTime windowStartTime, LocalDateTime windowEndTime, int count) {
            this.windowStartTime = windowStartTime;
            this.windowEndTime = windowEndTime;
            this.count = count;
        }
    }

    static class RateLimitingService implements IRateLimiterService {

        RateLimitingConfiguration rateLimitingConfiguration;
        IRateLimitingStrategy rateLimitingStrategy;

        public RateLimitingService(RateLimitingConfiguration rateLimitingConfiguration, IRateLimitingStrategy rateLimitingStrategy) {
            this.rateLimitingConfiguration = rateLimitingConfiguration;
            this.rateLimitingStrategy = rateLimitingStrategy;
        }


        @Override
        public Response validateRequest(Request request) {
            return rateLimitingStrategy.evaluateRequest(request);
        }
    }


    //Sliding window{
        //Queue
        // Pop----queue----push, remove till (current-Delta) time elements then calcuate request status
    //}

    //Token Bucket{
        // requet <<--pop----Queue<<-----schedular--push token startTime
        // queue pop - empty-- reject request
        // Optimized
        // Window (lastRefilTime, tokenCOunt)
        // do count-- while using tokens

    //}
    //Leaky Bucket{
        // request---> push in queue and wait --- schedular pick at rixed rate and call
    //}



}
