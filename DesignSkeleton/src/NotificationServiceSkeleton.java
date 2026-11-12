import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NotificationServiceSkeleton {

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
}
