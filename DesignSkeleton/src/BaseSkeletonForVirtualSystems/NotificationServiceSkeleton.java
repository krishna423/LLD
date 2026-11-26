package BaseSkeletonForVirtualSystems;

import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class NotificationServiceSkeleton {


    //Flow --> client-> NotificationFramework (Async) -> userPreference+ RateLimiting, Priority --> sendNotificationToChannel ->Retry--> DLQ


    enum NotificationChannel{
        SMS, EMail,Push,WhatsApp
    }

    interface IUserPreferenceService{
        boolean isEnabled(int userId, NotificationChannel notificationChannel);
    }

    interface IRateLimitingService{
        boolean isAllowed(int userId, NotificationChannel notificationChannel);
    }

    interface ITemplateService{
        Template getTemplate(String templateId);
        void addTemplate(Template template);
    }

    interface NotificationProviderStrategy{
        void sendNotification(Notification notification);
    }

    interface INotificationService{
         NotificationResponse sendNotification(NotificationRequest notificationRequest);
         //Push in blockingQueue(Thread safe) or priorityQueue(Use ReentrantLock to make thread safe)
         // INotificationQueue --> implemented blocking queue behind the scene
    }

    interface INotificationQueue {
        void enqueue(NotificationRequest request);
        NotificationRequest take() throws InterruptedException;
        //Push in blockingQueue(Thread safe) or priorityQueue(Use ReentrantLock to make thread safe)
    }

    //consumer or scheduler
    interface INotificationProcessor{
        NotificationRequest processNotification();
        //pick request from queue
        //Pick corresponding template
        //check UserPreference
        //Check rateLimiting
        //compiled template message-- strategy (NotificationRequest-->Notification)
        //Handover to notification Provider

        //Constructor
        //INotificationQueue
        //Map<NotificationChannel,NotificationProviderStrategy>
        //UserPreference
        //rateLimiting
        //templateService
        //IRenderingStrategy
        //NotificationProviderStrategy
    }

    interface IRenderingStrategy{
        NotificationTextContent compileMessage(NotificationRequest notificationRequest);
    }

    interface IRetryStrategy{
        //RetryQueue
        //DLQ

        void retry(Notification  notification);
        //if counter exhausted---> push in DLQ
    }


    static class Template {
        int id;
        String name;
        int priority;
        String templateText;
        String EmailSubjectText;
        String createdBy;
        Instant updatedAt;
    }

    static class Notification {
        NotificationTextContent notificationTextContent;
        String retryCount;
        NotificationChannel notificationChannel;
    }

    static class SMSProviderStrategy implements NotificationProviderStrategy{

        @Override
        public void sendNotification(Notification notification) {
            //Send notification code
            try{
                //Sucess
            }catch (Exception e){
                //Failure
                notification.retryCount = notification.retryCount+1;
                //push in RetryQueue
            }
        }
    }

    static class InMemoryNotificationQueue implements INotificationQueue {

        BlockingQueue<NotificationRequest> queue = new PriorityBlockingQueue<>(
                        100,Comparator.comparing(NotificationRequest::getTemplatePriority));

        ReentrantLock reentrantLock = new ReentrantLock();

        @Override
        public void enqueue(NotificationRequest request) {
            queue.add(request);

            //PriorityQueue--logic
            try{
                reentrantLock.lock();
                queue.add(request);
            } finally {
                reentrantLock.unlock();
            }

        }

        @Override
        public NotificationRequest take()
                throws InterruptedException {
            return queue.take();
        }
    }

    static class NotificationRequest {
        String templateName ;
        NotificationChannel notificationChannel;
        Map<String,String> templateValues ;
        int receiptId;
        @Getter
        transient int templatePriority ;

    }

    private static class NotificationResponse {
        String notificationStatus; //Accepted
        String notificationId;
    }


    static class NotificationTextContent {
        String compiledMessage;
        String compiledSubject;

    }
}
