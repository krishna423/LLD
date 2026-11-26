package BaseSkeletonForVirtualSystems;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class JobSchedulerSkeleton {


    // Flow---> client--> JobScheduler-->imMemory(JobQueue)--> JobExecuter-->executeJob


    //Interfaces
    interface IJobScheduler{
        ScheduledJob scheduleJob(Job job);
        boolean cancelJob(String jobId);
        void start();
        void shutdown();

        //IJobQueue.poll()-->IJobExecuter.execute()
    }

    interface IJobQueue{
        void add(ScheduledJob scheduledJob);
        ScheduledJob pollDueJob();

    }

    interface IJobExecuter{
        void execute(ScheduledJob scheduledJob);
        //FixedThreadPoolExecuter
    }


    //Models
    static class SchedulerConfig{
        int ExecuterThreadPoolSize;
    }

    static class Job {
        String jobId;
        String jobName;
        Instant executionTime;
        String callbackUrl;
    }

    static class ScheduledJob{
        Job job;
        String jobStatus;
        String createdAt;
        int retryCount;
    }


    //Classes
    static class JobExecuter implements IJobExecuter{

        SchedulerConfig schedulerConfig;
        ExecutorService executorService ;

        public JobExecuter(SchedulerConfig schedulerConfig) {
            this.schedulerConfig = schedulerConfig;
            this.executorService = Executors.newFixedThreadPool(schedulerConfig.ExecuterThreadPoolSize);;
        }

        @Override
        public void execute(ScheduledJob scheduledJob) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        //make Http call to callback;
                        scheduledJob.jobStatus="SUCCESS";
                    }catch (Exception e){
                        scheduledJob.jobStatus="FAILED";
                    }
                }
            });
        }
    }

    static class PriorityJobQueue implements IJobQueue{

        // constructor
        ReentrantLock reentrantLock = new ReentrantLock();
        PriorityQueue<ScheduledJob> pq = new PriorityQueue<>(new Comparator<ScheduledJob>() {
            @Override
            public int compare(ScheduledJob o1, ScheduledJob o2) {

                if(o1.job.executionTime.isBefore(o2.job.executionTime)) return -1;
                if(o1.job.executionTime.isAfter(o2.job.executionTime)) return 1;
                return 0;


            }
        });

        //PriorityQueue<ScheduledJob> pqTemp = new PriorityQueue<>(Comparator.comparing(x->x.executionTime));

        @Override
        public void add(ScheduledJob scheduledJob) {
            try{
                reentrantLock.lock();
                pq.add(scheduledJob);
            }finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public ScheduledJob pollDueJob() {
            ScheduledJob job = null;
            try{
                reentrantLock.lock();
                 job = pq.peek();

                 if(job.job.executionTime.isAfter(Instant.now())){
                     return null;
                }
                else{
                    job = pq.poll();
                }
            }
            finally {
                reentrantLock.unlock();

            }
            return job;

        }
    }

    static class JobScheduler implements IJobScheduler{

        IJobExecuter jobExecuter;
        IJobQueue jobQueue;

        @Override
        public ScheduledJob scheduleJob(Job job) {
            //Push in Queue
            // return ScheduledJob
            return null;
        }

        @Override
        public boolean cancelJob(String jobId) {
            //Create Map -- add mapping in map-- fetch from map
            return false;
        }

        @Override
        public void start() {
            Thread schedulerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        ScheduledJob scheduledJob = jobQueue.pollDueJob();
                        if(scheduledJob==null) continue;
                        jobExecuter.execute(scheduledJob);


                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }

        @Override
        public void shutdown() {

        }
    }


}
