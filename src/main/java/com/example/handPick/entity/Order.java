package com.example.handPick.entity; // Adjusted package name

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders") // Maps to the 'orders' table in the database
@Setter
@Getter // Lombok annotations to automatically generate getters and setters
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private Long id;

    @Column(name = "order_tracking_number")
    private String orderTrackingNumber;

    @Column(name = "total_quantity")
    private int totalQuantity;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "status")
    private String status;

    @Column(name = "date_created")
    @CreationTimestamp // Automatically sets the timestamp when the entity is created
    private Date dateCreated;

    @Column(name = "last_updated")
    @UpdateTimestamp // Automatically updates the timestamp when the entity is modified
    private Date lastUpdated;

    // Defines a one-to-many relationship with OrderItem entity.
    // CascadeType.ALL means that persistence operations (like persist, merge, remove)
    // applied to Order will cascade to its associated OrderItem entities.
    // 'mappedBy = "order"' indicates that the 'order' field in the OrderItem entity
    // is the owning side of this relationship.
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order")
    private Set<OrderItem> orderItems = new HashSet<>(); // Initialize to prevent NullPointerException

    // Defines a many-to-one relationship with the Customer entity.
    // @JoinColumn specifies the foreign key column in the 'orders' table that
    // references the 'customer' table.
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Defines a one-to-one relationship with the Address entity for shipping.
    // CascadeType.ALL means the shipping address will be saved/updated/deleted with the order.
    // @JoinColumn specifies the foreign key column in the 'orders' table.
    // 'referencedColumnName = "id"' explicitly states which column in the Address table is referenced.
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "shipping_address_id", referencedColumnName = "id")
    private Address shippingAddress;

    // Defines a one-to-one relationship with the Address entity for billing.
    // Similar to shippingAddress.
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "billing_address_id", referencedColumnName = "id")
    private Address billingAddress;

    // Helper method to add an OrderItem to the order's set of items
    // and establish the bidirectional relationship.
    public void add(OrderItem item) {
        if (item != null) {
            if (orderItems == null) {
                orderItems = new HashSet<>(); // Ensure the set is initialized
            }
            orderItems.add(item);
            item.setOrder(this); // Set the order on the order item side of the relationship
        }
    }
}