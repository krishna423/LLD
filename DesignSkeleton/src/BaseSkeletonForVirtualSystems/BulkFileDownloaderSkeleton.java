package BaseSkeletonForVirtualSystems;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BulkFileDownloaderSkeleton {

    // 1 Million files
    //Flow  Client ---> BlobDownloadSDK --> getSasUrl, ListMetaData in pages , Queue, Download file by file
    //Checkpoint manager


    interface ISASTokenProvider {
        SasToken getSASUrl(String folderPath);
        SasToken refreshSASUrl(String folderPath);
    }

    interface IBlobMetadataFetchService {
        void startJob(ScheduledJob job, SasToken sasToken);
    }

    interface IDownloadWorker extends Runnable {
        //run method
        //download file one by one
    }

    //Coordinate the complete operation
    interface IBlobDownloadSDK {
        DownloadResponse startDownload(DownloadRequest request);
        //Get SAS URL -ISASTokenProvider
        //Get Metadata - IBlobMetadataFetchService
        //Push in Queue - IDownloadQueue
        //Consume from queue and download blobs - IDownloadWorker
    }

    //Rest Call to Azure Blob Storage
    interface IBlobStorageClient {
        BlobMetaDataDetailsPage listBlobPages(SasToken sasToken, String folderPath, String continuationToken, int pageSize);
        void downloadBlob(SasToken sasToken, String blobName, String expectedETag, Path destination);
    }

    interface IDownloadQueue {
        void submit(DownloadTask task) throws InterruptedException;
        DownloadTask take() throws InterruptedException;
        //Powered by blocking queue -- Input-output controlled
    }

    interface ICheckpointManager {
        boolean isDownloaded(String blobName, String etag);
        void markCompleted(String blobName, String etag);
        void markFailed(String blobName, String reason);

    }

    interface IRetryPolicy {
        boolean shouldRetry(Exception exception, int attempt);
        long getRetryDelayMillis(int attempt);
    }

    enum JobStatus {
        CREATED, RUNNING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, CANCELLED
    }

    enum DownloadTaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, VERSION_CONFLICT
    }

     static class DownloadRequest {
         String folderPath;
         String localDestination;
         int workerCount;
         int pageSize;

         public DownloadRequest(String folderPath, String localDestination, int workerCount, int pageSize) {
             this.folderPath = folderPath;
             this.localDestination = localDestination;
             this.workerCount = workerCount;
             this.pageSize = pageSize;
         }
     }
     static class SasToken {

         String sasUrl;
         String folderPath;
         Instant expiryTime;
    }

     static class BlobMetadata {

         String blobName;
         long size;
         String etag;
    }

     static class BlobMetaDataDetailsPage {

         List<BlobMetadata> blobs;
         String continuationToken;
    }

     static class DownloadTask {
         String blobName;
         long fileSize;
         String etag;
         int retryCount;
         DownloadTaskStatus status;

         public DownloadTask(BlobMetadata metadata) {
             this.blobName = metadata.blobName;
             this.fileSize = metadata.size;
             this.etag = metadata.etag;
             this.retryCount = 0;
             this.status = DownloadTaskStatus.PENDING;
         }


    }
     static class ScheduledJob {

         String jobId;
         DownloadRequest request;
         volatile JobStatus status;

         public ScheduledJob(String jobId, DownloadRequest request) {
             this.jobId = jobId;
             this.request = request;
         }
     }

     static class DownloadResponse {

         String jobId;
         JobStatus status;

         public DownloadResponse(String jobId, JobStatus status) {
             this.jobId = jobId;
             this.status = status;
         }
     }








//Azure Rest client call
    static class AzureBlobStorageClient implements IBlobStorageClient {

        @Override
        public BlobMetaDataDetailsPage listBlobPages(SasToken sasToken, String folderPath, String continuationToken, int pageSize) {
            // 1. Create BlobContainerClient using SAS URL.
            // 2. List blobs with prefix = folderPath.
            // 3. Set max results = pageSize.
            // 4. Pass continuationToken to Azure.
            return null;
        }

        @Override
            public void downloadBlob(SasToken sasToken, String blobName, String expectedETag, Path destination) {

                //Download file using stream
                // 1. Create BlobClient using SAS URL.
                // 2. Set BlobRequestConditions with If-Match = expectedETag.
            }
    }


//Producer
     static class BlobMetadataFetchService implements IBlobMetadataFetchService {

        private final IBlobStorageClient storageClient;
        private final IDownloadQueue downloadQueue;

        public BlobMetadataFetchService(IBlobStorageClient storageClient, IDownloadQueue downloadQueue) {
            this.storageClient = storageClient;
            this.downloadQueue = downloadQueue;
        }

        @Override
        public void startJob(ScheduledJob scheduledJob, SasToken sasToken) {

            String continuationToken = null;

            try {
                do {
                    BlobMetaDataDetailsPage page = storageClient.listBlobPages(sasToken, scheduledJob.request.folderPath, continuationToken, scheduledJob.request.pageSize);

                    for (BlobMetadata blobMetadata : page.blobs) {
                        DownloadTask task = new DownloadTask(blobMetadata);
                        downloadQueue.submit(task);
                    }

                    // Save the next token returned by Azure.
                    continuationToken = page.continuationToken;

                } while (continuationToken != null && !continuationToken.isEmpty());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Listing interrupted", e);
            }
        }
    }

//Consumer
    static class DownloadWorker implements IDownloadWorker {

        private final IDownloadQueue queue;
        private final IBlobStorageClient storageClient;
        private final ICheckpointManager checkpointManager;
        private final SasToken sasToken;

        public DownloadWorker(
                IDownloadQueue queue,
                IBlobStorageClient storageClient,
                ICheckpointManager checkpointManager,
                SasToken sasToken){

            this.queue = queue;
            this.storageClient = storageClient;
            this.checkpointManager = checkpointManager;
            this.sasToken = sasToken;
        }

        @Override
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    DownloadTask task = queue.take();

                    if (checkpointManager.isDownloaded(task.blobName, task.etag)) {
                        continue;
                    }
                    processTask(task);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void processTask(DownloadTask task) {

            task.status = DownloadTaskStatus.PROCESSING;

            while (true) {
                try {
                    Path temporaryPath = Path.of("temporayPath");
                    Path finalPath = Path.of("actualPath");

                    Files.createDirectories(finalPath.getParent());

                    storageClient.downloadBlob(sasToken, task.blobName, task.etag, temporaryPath);
                    Files.copy(temporaryPath, finalPath);

                    checkpointManager.markCompleted(task.blobName, task.etag);
                    task.status = DownloadTaskStatus.COMPLETED;

                    return;

                } catch (RuntimeException e) {

                    task.status = DownloadTaskStatus.VERSION_CONFLICT;
                    checkpointManager.markFailed(task.blobName, e.getMessage());
                    return;

                } catch (Exception e) {

                }
            }
        }
    }

    static class BlobDownloadSDK implements IBlobDownloadSDK {

        private final ISASTokenProvider sasTokenProvider;
        private final IBlobStorageClient storageClient;
        private final IBlobMetadataFetchService blobDownloaderService;
        private final IDownloadQueue queue;
        private final ICheckpointManager checkpointManager;
        private final IRetryPolicy retryPolicy;

        private final Map<String, ScheduledJob> jobs = new ConcurrentHashMap<>();

        public BlobDownloadSDK(
                ISASTokenProvider sasTokenProvider,
                IBlobStorageClient storageClient,
                IBlobMetadataFetchService blobDownloaderService,
                IDownloadQueue queue,
                ICheckpointManager checkpointManager,
                IRetryPolicy retryPolicy) {

            this.sasTokenProvider = sasTokenProvider;
            this.storageClient = storageClient;
            this.blobDownloaderService = blobDownloaderService;
            this.queue = queue;
            this.checkpointManager = checkpointManager;
            this.retryPolicy = retryPolicy;
        }

        @Override
        public DownloadResponse startDownload(DownloadRequest request) {

            String jobId = UUID.randomUUID().toString();

            ScheduledJob scheduledJob = new ScheduledJob(jobId, request);
            scheduledJob.status = JobStatus.RUNNING;

            jobs.put(jobId, scheduledJob);
            SasToken sasToken = sasTokenProvider.getSASUrl("");

            ExecutorService executor = Executors.newFixedThreadPool(request.workerCount);
            List<Future<?>> workerFutures = new ArrayList<>();

            try {
                // Start consumers first.
                for (int i = 0; i < request.workerCount; i++) {
                    DownloadWorker worker = new DownloadWorker(queue, storageClient, checkpointManager,sasToken);
                    workerFutures.add(executor.submit(worker));
                }

                // Start process Job
                blobDownloaderService.startJob(scheduledJob, sasToken);
                // Tell all workers that no more tasks will arrive.
                // Add position Pill to stop workers

                // Wait until all workers finish.
                for (Future<?> future : workerFutures) {
                    future.get();
                }

            } catch (Exception e) {
                scheduledJob.status = JobStatus.FAILED;
                throw new RuntimeException("Download scheduledJobs failed: " + jobId, e);
            } finally {
                executor.shutdownNow();
            }
            return new DownloadResponse(jobId, scheduledJob.status);
        }
    }

    public class DownloadApplication {

        public static void main(String[] args) {

            // 1. Create infrastructure components

//            IDownloadQueue queue = new InMemoryDownloadQueue(1000);
//            ICheckpointManager checkpointManager = new InMemoryCheckpointManager();
//            IRetryPolicy retryPolicy = new DefaultRetryPolicy();
//            IBlobStorageClient storageClient = new AzureBlobStorageClient();
//            ISasTokenProvider sasTokenProvider = new SASTokenProvider();
//
//            // 2. Create DownloaderService service
//            IBlobDownloaderService listingService = new BlobDownloaderService(storageClient, queue);
//
//            // 3. Create SDK
//            IBlobDownloadSDK blobDownloadSDK = new BlobDownloadSDK(sasTokenProvider, storageClient, listingService, queue, checkpointManager, retryPolicy);
//
//            // 4. Create request
//            DownloadRequest request = new DownloadRequest("folder-B/", "/downloads", 10, 1000);
//
//            // 5. Start download
//            DownloadResponse response = coordinator.startDownload(request);
//            System.out.println("Job ID: " + response.jobId + "   Status " + response.status );
        }
    }

}
