import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JobSchedulerSkeleton {

    // ============================================================
    // 10. JOB SCHEDULER / TASK EXECUTION FRAMEWORK
    // ============================================================

    enum JobStatus { SCHEDULED, QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }
    enum JobType { ONE_TIME, RECURRING }

    static class Job {
        private String jobId;
        private JobType type;
        private JobStatus status;
        private Instant scheduledAt;
        private int priority;
        private int maxRetries;
    }

    interface JobTask {
        void execute(JobExecutionContext context) throws Exception;
    }

    interface JobScheduler {
        String schedule(JobDefinition definition);
        boolean cancel(String jobId);
        Optional<Job> getJob(String jobId);
    }

    interface JobExecutionStrategy {
        void execute(Job job);
    }

    interface JobRetryPolicy {
        boolean shouldRetry(Job job, Exception exception);
        Duration getRetryDelay(Job job, int attempt);
    }

    interface JobStore {
        void save(Job job);
        Optional<Job> findById(String jobId);
        List<Job> findDueJobs(Instant now);
        void updateStatus(String jobId, JobStatus status);
    }

    interface JobQueue {
        void enqueue(Job job);
        Optional<Job> dequeue();
    }

    static class JobDefinition {
        private JobTask task;
        private Instant scheduledAt;
        private JobType type;
        private int priority;
        private int maxRetries;
    }

    static class JobExecutionContext {
        private String jobId;
        private int attempt;
        private Map<String, Object> attributes;
    }
}
