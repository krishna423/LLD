import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FoodOrderingDeliverySkeleton {

    // ============================================================
    // 4. FOOD ORDERING AND DELIVERY SYSTEM
    // ============================================================

    enum OrderStatus {
        CREATED, ACCEPTED, PREPARING, READY_FOR_PICKUP,
        PICKED_UP, DELIVERED, CANCELLED
    }

    static class Restaurant {
        private String restaurantId;
        private String name;
        private List<MenuItem> menu;
    }

    static class MenuItem {
        private String itemId;
        private String name;
        private double price;
    }

    static class FoodOrder {
        private String orderId;
        private String customerId;
        private String restaurantId;
        private List<OrderLine> items;
        private OrderStatus status;
    }

    static class OrderLine {
        private MenuItem item;
        private int quantity;
    }

    static class DeliveryPartner {
        private String partnerId;
        private boolean available;
    }

    interface DeliveryAssignmentStrategy {
        Optional<DeliveryPartner> assignPartner(FoodOrder order, List<DeliveryPartner> partners);
    }

    interface DiscountStrategy {
        double calculateDiscount(FoodOrder order);
    }

    interface FoodOrderService {
        FoodOrder placeOrder(String customerId, String restaurantId, List<OrderLine> items);
        void updateOrderStatus(String orderId, OrderStatus status);
        void cancelOrder(String orderId);
        Optional<FoodOrder> getOrder(String orderId);
    }

    interface OrderStatusListener {
        void onStatusChanged(FoodOrder order, OrderStatus newStatus);
    }
}
