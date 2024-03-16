package FoodOrderSystem.src;


import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}

class FoodOrderSystem {
    String systemName;
    List<Customer> customerList;
    List<Restuarant> getCustomerList;

    Order placeOrder(Cart cart){return new Order(null,null);}

    Cart getCart() { return  new Cart(null,null);}
}

class Cart {
    Customer customer;
    List<FoodItemDetail> foodItemDetailList;

    public Cart(Customer customer, List<FoodItemDetail> foodItemDetailList) {
        this.customer = customer;
        this.foodItemDetailList = foodItemDetailList;
    }

    void addFood(FoodItemDetail foodItemDetail, int qty){};

    void remmoveFood(FoodItemDetail foodItemDetail, int qty){};

    void editFood(FoodItemDetail foodItemDetailList){};

}

class Person {
    int age;
    String name;
}

class Customer extends Person {
    Account account;
    Cart cart;
    List<Order> orderList;
}

class FoodItemDetail {
    String id;
    FoodItem foodItem;
    Restuarant restuarant;
    int qty;

}

class Order {
    String orderId;
    List<FoodItemDetail> foodItemDetailList;

    public Order(String orderId, List<FoodItemDetail> foodItemDetailList) {
        this.orderId = orderId;
        this.foodItemDetailList = foodItemDetailList;
    }
}

class Account {
    String username;
    String password;
    Status status;
}

enum Status {
    ACTIVE, BLOCKED
}

class Restuarant {
    String restuarantId;
    Address address;
    Menu menu;
    int rating;
}

class Menu {
    List<FoodItem> foodItemList;
}

class FoodItem {
    String foodId;
    String description;
    double price;
}

class Address {
    String pincode;
    String street;
    String city;
    String state;
}


