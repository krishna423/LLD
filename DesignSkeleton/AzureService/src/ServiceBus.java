import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServiceBus {

    // ============================================================
    // 1. PUB-SUB (AZURE SERVICE BUS) SYSTEM
    // ============================================================


    // 1. --------- Architectural Thinking -----------------------
    //
    // I'll first break the Pub-Sub system into four major
    // responsibilities:
    //
    // 1. Message publishing
    // 2. Message routing and subscription management
    // 3. Message consumption
    // 4. Message acknowledgment and retry management
    //
    // Then I'll identify the entities involved in each
    // responsibility and define how they interact.


    // 2. --------- Identify Domain Objects ----------------------
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


    // ============================================================
    // 1. MESSAGE PUBLISHING
    // ============================================================

    static class Message {

        private String messageId;
        private String payload;
        private Map<String, Object> properties;
        private Instant createdAt;
    }

    interface MessagePublisher {

        void publish(String topicName, Message message);
    }


    // ============================================================
    // 2. MESSAGE ROUTING / SUBSCRIPTION MANAGEMENT
    // ============================================================

    static class Topic {

        private String topicName;
        private List<Subscription> subscriptions;
    }

    static class Queue {

        private String queueName;
    }

    static class Subscription {

        private String subscriptionName;
        private String topicName;
        private MessageFilter messageFilter;
    }

    interface MessageFilter {

        boolean matches(Message message);
    }

    // Strategy Pattern:
    // Different filters can be implemented for subscriptions.

    static class OrderTypeFilter implements MessageFilter {

        @Override
        public boolean matches(Message message) {
            // Check message properties
            return true;
        }
    }


    // ============================================================
    // 3. BROKER MANAGEMENT
    // ============================================================

    interface MessageBroker {

        void createTopic(String topicName);

        void createSubscription(
                String topicName,
                String subscriptionName
        );

        void publish(String topicName, Message message);
    }

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


    // ============================================================
    // 4. MESSAGE CONSUMPTION
    // ============================================================

    interface MessageConsumer {

        Optional<MessageDelivery> receive(String subscriptionName);
    }

    interface MessageHandler {

        void handle(Message message);
    }

    static class MessageDelivery {

        private Message message;
        private String lockToken;
        private int deliveryCount;
        private Instant lockExpiryTime;
    }

    // Different consumers can implement different business logic.

    static class PaymentConsumer implements MessageHandler {

        @Override
        public void handle(Message message) {
            // Process payment event
        }
    }

    static class EmailConsumer implements MessageHandler {

        @Override
        public void handle(Message message) {
            // Send confirmation email
        }
    }


    // ============================================================
    // 5. MESSAGE ACKNOWLEDGMENT / SETTLEMENT
    // ============================================================

    interface MessageSettlement {

        void complete(String lockToken);

        void abandon(String lockToken);

        void deadLetter(String lockToken, String reason);
    }

    // Retry Strategy Pattern:
    // Retry logic can change independently.

    interface RetryPolicy {

        boolean shouldRetry(int deliveryCount);
    }

    static class ExponentialRetryPolicy implements RetryPolicy {

        @Override
        public boolean shouldRetry(int deliveryCount) {
            return deliveryCount < 5;
        }
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

    interface PubSubService {

        // Publisher sends a message to a topic.
        void publish(String topicName, Message message);

        // Register consumer for a topic subscription.
        void subscribe(
                String topicName,
                String subscriptionName,
                MessageHandler handler
        );

        // Consumer acknowledges successful processing.
        void acknowledge(String lockToken);

        // Consumer requests redelivery.
        void retry(String lockToken);
    }


    // Facade Pattern:
    // PubSubService provides a simple interface to the
    // underlying publishing, routing and consumption logic.

    static class PubSubServiceImpl implements PubSubService {

        private MessageBroker messageBroker;
        private MessagePublisher messagePublisher;
        private MessageSettlement messageSettlement;
        private RetryPolicy retryPolicy;

        @Override
        public void publish(String topicName, Message message) {
            // Validate message
            // Delegate publishing to MessagePublisher
            // Route message to the topic
        }

        @Override
        public void subscribe(
                String topicName,
                String subscriptionName,
                MessageHandler handler
        ) {
            // Register consumer for subscription
            // Receive messages
            // Delegate business processing to handler
        }

        @Override
        public void acknowledge(String lockToken) {
            // Complete message using settlement service
        }

        @Override
        public void retry(String lockToken) {
            // Abandon message or apply retry policy
        }
    }

}

