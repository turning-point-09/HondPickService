package com.example.handPick.service; // Adjusted package name to match your project structure

import com.example.handPick.dao.CustomerRepository;      // Adjusted import
import com.example.handPick.dto.PaymentInfo;            // Adjusted import
import com.example.handPick.dto.Purchase;               // Adjusted import
import com.example.handPick.dto.PurchaseResponse;       // Adjusted import
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired; // Can be omitted in modern Spring if only one constructor
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CustomerRepository customerRepository; // Made final for good practice

    // @Autowired can be omitted in Spring Boot 2.x/3.x if there's only one constructor
    public CheckoutServiceImpl(CustomerRepository customerRepository, @Value("${stripe.key.secret}") String secretKey) {
        this.customerRepository = customerRepository;

        // init Stripe API with secretKey
        Stripe.apiKey = secretKey;
    }

    @Override
    @Transactional // Ensures atomicity for database operations
    public PurchaseResponse placeOrder(Purchase purchase) {

        // Retrieve the order info from DTO
        var order = purchase.getOrder();

        // Generate a unique order tracking number
        var orderTrackingNumber = generateOrderTrackingNumber();
        order.setOrderTrackingNumber(orderTrackingNumber);

        // Populate order with orderItems
        var orderItems = purchase.getOrderItems();
        orderItems.forEach(item -> order.add(item)); // Assuming Order.add(OrderItem) exists and handles bi-directional relationship

        // Populate order with billingAddress and shippingAddress
        order.setBillingAddress(purchase.getBillingAddress());
        order.setShippingAddress(purchase.getShippingAddress());

        // Populate customer with order
        var customer = purchase.getCustomer();

        // Check if this is an existing customer (based on customer email)
        var email = customer.getEmail();
        var customerFromDB = customerRepository.findByEmail(email);

        if (customerFromDB != null) {
            customer = customerFromDB; // Use the existing customer if found
        }

        customer.add(order); // Assuming Customer.add(Order) exists and handles bi-directional relationship

        // Save to the database (Cascades save to Order and OrderItems)
        customerRepository.save(customer);

        // Return a response with the order tracking number
        return new PurchaseResponse(orderTrackingNumber);
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException {

        // Stripe API parameters
        var paymentMethodTypes = new ArrayList<>();
        paymentMethodTypes.add("card"); // Only 'card' payment method type is specified

        var params = new HashMap<String, Object>();
        params.put("amount", paymentInfo.getAmount());
        params.put("currency", paymentInfo.getCurrency());
        params.put("payment_method_types", paymentMethodTypes);
        params.put("description", "Hand Pick purchase"); // Changed description for consistency
        params.put("receipt_email", paymentInfo.getReceiptEmail());
        // params.put("setup_future_usage", "off_session"); // Optional: if you intend to save cards for later use

        return PaymentIntent.create(params);
    }

    // Helper method to generate a unique order tracking number
    private String generateOrderTrackingNumber() {
        // Generates a random UUID (Universally Unique Identifier) string
        return UUID.randomUUID().toString();
    }
}