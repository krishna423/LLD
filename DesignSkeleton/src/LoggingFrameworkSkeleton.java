import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LoggingFrameworkSkeleton {

    // ============================================================
    // 6. LOGGING FRAMEWORK
    // ============================================================

    enum LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

    static class LogEvent {
        private Instant timestamp;
        private LogLevel level;
        private String loggerName;
        private String message;
        private Throwable throwable;
    }

    interface Logger {
        void log(LogLevel level, String message);
        void log(LogLevel level, String message, Throwable throwable);
    }

    interface LogAppender {
        void append(LogEvent event);
    }

    interface LogFormatter {
        String format(LogEvent event);
    }

    interface LogFilter {
        boolean shouldLog(LogEvent event);
    }

    interface LoggingConfiguration {
        LogLevel getMinimumLevel();
        List<LogAppender> getAppenders();
        LogFormatter getFormatter();
    }
}
