import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServiceBus {


    //Flow --> client --> publish message -->  Queue ---> consumer --> MessageHandler , send ack --> DLQ


    // 1. --------- Architectural Thinking -----------------------
    //
    // I'll first break the Pub-Sub system into four major responsibilities/ user or system activities and identity components:
    //
    // 1. Message publishing
    // 2. Message routing and subscription management
    // 3. Message consumption
    // 4. Message acknowledgment and retry management





    interface MessageBroker {
        void createTopic(String topicName);
        void createSubscription(String topicName, String subscriptionName);
        void publish(String topicName, Message message);
        //IQueueManager
    }

    static class IQueueManager {
        private Map<String, Topic> topicMap;
    }

    interface MessagePublisher {
        void publish(String topicName, Message message);
        //use MessageBroker
    }

    interface MessageHandler {
        void handle(Message message);
    }

    interface MessageSettlement {
        void complete(String lockToken);
        void abandon(String lockToken);
        void deadLetter(String lockToken, String reason);
    }

    interface MessageConsumer {
        Optional<MessageDelivery> receive(String subscriptionName);
        //IQueueManager
        //MessageHandler
        //MessageSettlement
        //Retry Logic
         //mm.complete(MessageDelivery.lockToken())
    }

    static class MessageDelivery {
        private Message message;
        private String lockToken;
        private int deliveryCount;
        private Instant lockExpiryTime;
    }

    static class Message {
        private String messageId;
        private String payload;
        private Map<String, Object> properties;
        private Instant createdAt;
    }

    static class Topic {
        private String topicName;
        private List<Subscription> subscriptions;
    }


    static class Subscription {
        private String subscriptionName;
        private String topicName;
    }


    // Retry Strategy Pattern:
    // Retry logic can change independently.

    interface RetryPolicy {
        boolean shouldRetry(int deliveryCount);
        //DeadLetterQueue
    }


    static class DeadLetterQueue {
        private String queueName;
        void add(Message message, String reason) {
            // Move message to DLQ
        }
    }


    // ============================================================
    // 6. ORCHESTRATION LAYER
    // ============================================================
//    class Main {
//
//        public static void main(String[] args) {
//
//        }
//
//    }





//    ------------------------------------- Not required ------------------------------------------

    // Adapter Pattern:
    // Actual Azure Service Bus SDK implementation can be
    // hidden behind this interface.

    static class AzureServiceBusAdapter implements MessageBroker {

        @Override
        public void createTopic(String topicName) {
            // Create Azure Service Bus topic
        }

        @Override
        public void createSubscription(
                String topicName,
                String subscriptionName
        ) {
            // Create subscription under the topic
        }

        @Override
        public void publish(String topicName, Message message) {
            // Send message using Azure Service Bus SDK
        }
    }


    // 2. --------- Identify Domain/corressponding Objects ----------------------
    //
    // 1. Publishing       -- Message, MessagePublisher
    // 2. Routing          -- Topic, Queue, Subscription, Broker
    // 3. Consumption      -- MessageConsumer, MessageHandler
    // 4. Acknowledgment   -- MessageDelivery, MessageSettlement
    // 5. Retry            -- RetryPolicy, DeadLetterQueue


    // 3. --------- Identify Orchestration Layer -----------------
    //
    // PubSubService coordinates publishing and consumption.
    //
    // Publisher sends a message to a topic.
    // Broker routes the message to matching subscriptions.
    // Consumer receives and processes the message.
    // Consumer acknowledges successful processing.
    // Failed messages can be retried or moved to DLQ.


    // 4. --------- Identify What May Change ---------------------
    //                 Open-Closed Principle
    //
    // - Routing: Queue-based or Topic-based.
    // - Filtering: Different subscription filters.
    // - Retry: Fixed delay or Exponential backoff.
    // - Delivery: Peek-Lock or Receive-and-Delete.
    // - Message handling: Different consumers for events.
    // - Broker: Azure Service Bus or another messaging provider.

}

