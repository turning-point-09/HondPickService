package com.example.handPick.controller; // Adjusted package name

import com.example.handPick.dto.PaymentInfo;    // Adjusted import
import com.example.handPick.dto.Purchase;       // Adjusted import
import com.example.handPick.dto.PurchaseResponse; // Adjusted import
import com.example.handPick.service.CheckoutService; // Adjusted import
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin // Added for explicit CORS enablement, relies on AppConfig for specific rules
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    private final CheckoutService checkoutService; // Use 'final' for injected dependencies

    // Constructor injection is the recommended way
    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponse> placeOrder(@RequestBody Purchase purchase) {
        PurchaseResponse purchaseResponse = checkoutService.placeOrder(purchase);
        // Return 201 Created status for successful resource creation
        return new ResponseEntity<>(purchaseResponse, HttpStatus.CREATED);
    }

    @PostMapping("/payment-intent")
    public ResponseEntity<String> createPaymentIntent(@RequestBody PaymentInfo paymentInfo) {
        try {
            var paymentIntent = checkoutService.createPaymentIntent(paymentInfo);
            // It's generally better to return a DTO or a Map<String, String> containing the client_secret
            // rather than the raw JSON string of the entire PaymentIntent object.
            // For now, retaining the original intent of returning the JSON string.
            var paymentStr = paymentIntent.toJson();

            return new ResponseEntity<>(paymentStr, HttpStatus.OK);
        } catch (StripeException e) {
            // Log the detailed exception for debugging on the server side
            logger.error("Error creating payment intent for payment info: {}", paymentInfo, e);

            // Return an appropriate error response to the client
            // For production, consider a more structured error DTO (e.g., with error code, message)
            // and a more generic message to avoid exposing internal details.
            return new ResponseEntity<>("Error processing payment: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}