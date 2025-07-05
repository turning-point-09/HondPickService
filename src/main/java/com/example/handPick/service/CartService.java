package com.example.handPick.service;

import com.example.handPick.controller.AuthController;
import com.example.handPick.dto.AddToCartRequest;
import com.example.handPick.dto.CartDto;
import com.example.handPick.dto.CartItemDto;
import com.example.handPick.dto.CheckoutRequest;
import com.example.handPick.model.Cart;
import com.example.handPick.model.CartItem;
import com.example.handPick.model.Order;
import com.example.handPick.model.OrderItem;
import com.example.handPick.model.Product;
import com.example.handPick.model.User;
import com.example.handPick.model.ShippingAddress;
import com.example.handPick.repository.CartItemRepository;
import com.example.handPick.repository.CartRepository;
import com.example.handPick.repository.OrderItemRepository;
import com.example.handPick.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final UserService userService;

    @Value("${app.gst.rate:0.18}")
    private double gstRate;

    @Autowired
    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       OrderRepository orderRepository,
                       OrderItemRepository orderItemRepository,
                       ProductService productService,
                       UserService userService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
        this.userService = userService;
    }

    /**
     * Retrieves the active cart for a given user.
     * If no active cart is found, a new one is created.
     *
     * @param user The authenticated user.
     * @return A CartDto representing the active cart.
     * @throws IllegalArgumentException if user is null.
     */
    @Transactional
    public CartDto getCart(User user, UUID guestId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be logged in to get the cart.");
        }
        Cart cart = getCartEntityForAddOrUpdate(user);
        return convertToDto(cart);
    }

    /**
     * Adds an item to the cart for a user.
     * Manages stock deduction and updates existing cart items or creates new ones.
     *
     * @param user The authenticated user.
     * @param request AddToCartRequest containing product ID, quantity, and optional size.
     * @return A CartDto representing the updated cart.
     * @throws IllegalArgumentException if product is not found or stock is insufficient.
     */
    @Transactional
    public CartDto addItemToCart(User user, AddToCartRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be logged in to add items to the cart.");
        }
        if (request.getProductId() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Invalid product ID or quantity.");
        }

        Cart cart = cartRepository.findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCartForUser(user));
        Product product = productService.getProductEntityById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + request.getProductId()));

        // Check if an item with the same product ID AND size already exists in the cart
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()) &&
                        (request.getSize() == null ? i.getSize() == null : request.getSize().equals(i.getSize())))
                .findFirst();

        int addQty = request.getQuantity();
        int currentQty = existing.map(CartItem::getQuantity).orElse(0);
        int newTotal = currentQty + addQty;

        // Check for sufficient stock before adding
        if (product.getStockQuantity() != null && product.getStockQuantity() < newTotal) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " +
                            product.getStockQuantity() + ", requested: " + newTotal);
        }

        if (existing.isPresent()) {
            // Update quantity of existing item
            CartItem item = existing.get();
            item.setQuantity(newTotal);
            item.setPrice(product.getPrice()); // Ensure price is updated to current product price
            cartItemRepository.save(item);
        } else {
            // Add new item to cart
            CartItem item = new CartItem(product, addQty, cart, request.getSize()); // Pass size to constructor
            cart.getItems().add(item); // Add to cart's item list
            cartItemRepository.save(item); // Explicitly save the new item
        }

        // Deduct stock from the product
        if (product.getStockQuantity() != null) {
            product.setStockQuantity(product.getStockQuantity() - addQty);
            productService.saveProductEntity(product);
        }

        // The @PreUpdate in Cart entity will handle updatedAt, but saving explicitly ensures changes are flushed.
        cartRepository.save(cart);
        return convertToDto(cart);
    }

    /**
     * Updates the quantity of a specific item in the cart.
     * If quantity is 0 or less, the item is removed.
     *
     * @param user The authenticated user.
     * @param productId The ID of the product to update.
     * @param quantity The new quantity.
     * @return A CartDto representing the updated cart.
     * @throws IllegalArgumentException if the cart item is not found or stock is insufficient.
     */
    @Transactional
    public CartDto updateItemQuantity(User user, UUID guestId,
                                      Long productId, int quantity) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be logged in to update the cart.");
        }
        Cart cart = cartRepository.findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Active cart not found for user."));
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Cart item not found: " + productId));

        Product product = item.getProduct();
        int oldQty = item.getQuantity();
        int diff = quantity - oldQty; // Difference in quantity (positive for increase, negative for decrease)

        if (quantity <= 0) {
            // Remove item if new quantity is zero or less
            cart.getItems().remove(item); // Remove from cart's item list
            cartItemRepository.delete(item); // Delete the item from DB
            if (product.getStockQuantity() != null) {
                product.setStockQuantity(product.getStockQuantity() + oldQty); // Return old quantity to stock
                productService.saveProductEntity(product);
            }
        } else {
            // Update quantity
            if (diff > 0 // If quantity is increasing, check stock
                    && product.getStockQuantity() != null
                    && product.getStockQuantity() < diff) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Only " +
                                product.getStockQuantity() +
                                " more units available.");
            }
            item.setQuantity(quantity);
            item.setPrice(product.getPrice()); // Ensure price is updated to current product price
            cartItemRepository.save(item); // Save the updated item
            if (product.getStockQuantity() != null) {
                product.setStockQuantity(product.getStockQuantity() - diff); // Adjust stock based on difference
                productService.saveProductEntity(product);
            }
        }

        // The @PreUpdate in Cart entity will handle updatedAt, but saving explicitly ensures changes are flushed.
        cartRepository.save(cart);
        return convertToDto(cart);
    }

    /**
     * Removes a specific item from the cart.
     *
     * @param user The authenticated user.
     * @param productId The ID of the product to remove.
     * @return A CartDto representing the updated cart.
     * @throws IllegalArgumentException if the product is not found in the cart.
     */
    @Transactional
    public CartDto removeItemFromCart(User user, UUID guestId, Long productId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be logged in to update the cart.");
        }
        Cart cart = cartRepository.findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Active cart not found for user."));
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not in cart: " + productId));

        Product product = item.getProduct();
        cart.getItems().remove(item); // Remove from cart's item list
        cartItemRepository.delete(item); // Delete the item from DB

        // Return stock to the product
        if (product.getStockQuantity() != null) {
            product.setStockQuantity(
                    product.getStockQuantity() + item.getQuantity());
            productService.saveProductEntity(product);
        }

        // The @PreUpdate in Cart entity will handle updatedAt, but saving explicitly ensures changes are flushed.
        cartRepository.save(cart);
        return convertToDto(cart);
    }

    /**
     * Checks out the active cart for an authenticated user, creating an order.
     * Handles optional shipping address update and stock deduction.
     *
     * @param initialUser The authenticated user.
     * @param checkoutRequest CheckoutRequest containing payment method and optional shipping address.
     * @return The created Order entity.
     * @throws IllegalArgumentException if user is null, cart is empty, or stock is insufficient,
     * or if shipping address is required but not provided/found.
     */
    @Transactional
    public Order checkoutCart(User initialUser, CheckoutRequest checkoutRequest) {
        // Ensure user is authenticated
        if (initialUser == null) {
            throw new IllegalArgumentException(
                    "Authenticated user required for checkout.");
        }

        // Fetch the user again to ensure latest address details are loaded if updated
        User userForOrder = userService.findById(initialUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found during checkout."));

        // Update user's address if provided in checkout request
        if (checkoutRequest.getShippingAddress() != null) {
            userService.updateOrCreateUserAddress(userForOrder.getId(), checkoutRequest.getShippingAddress());
            // Re-fetch user to get the newly updated/created address attached.
            userForOrder = userService.findById(userForOrder.getId())
                    .orElseThrow(() -> new RuntimeException("User not found after address update."));
        }

        // Get the active cart for the user
        Cart activeCart = cartRepository
                .findByUserAndStatus(userForOrder, Cart.CartStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No active cart for user, or cart is not in ACTIVE status."));

        // Ensure cart is not empty
        if (activeCart.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot checkout an empty cart.");
        }

        // Validate shipping address presence and create a snapshot for the order
        ShippingAddress orderShippingAddress = new ShippingAddress();
        if (userForOrder.getAddress() != null) {
            orderShippingAddress.setStreet(userForOrder.getAddress().getStreet());
            orderShippingAddress.setCity(userForOrder.getAddress().getCity());
            orderShippingAddress.setState(userForOrder.getAddress().getState());
            orderShippingAddress.setPostalCode(userForOrder.getAddress().getPostalCode());
            orderShippingAddress.setCountry(userForOrder.getAddress().getCountry());
//            orderShippingAddress.setLandmark(userForOrder.getAddress().getLandmark());
//            orderShippingAddress.setPhoneNumber(userForOrder.getAddress().getPhoneNumber());
        } else {
            throw new IllegalArgumentException("Shipping address is required for checkout but not found for user.");
        }

        // Calculate final totals for the order, including GST
        BigDecimal subtotalBeforeTax = activeCart.getTotalPrice();
        BigDecimal gstAmount = subtotalBeforeTax.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotalBeforeTax.add(gstAmount);

        // Create the Order entity
        Order order = new Order();
        order.setUser(userForOrder);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(checkoutRequest.getPaymentMethod());
        order.setShippingAddress(orderShippingAddress);

        // Simulate payment status based on method
        if (checkoutRequest.getPaymentMethod() == Order.PaymentMethod.COD) {
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setStatus(Order.OrderStatus.PENDING);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        order.setOrderDate(LocalDateTime.now());
        order = orderRepository.save(order);

        // Process cart items into order items and deduct stock
        for (CartItem cartItem : new ArrayList<>(activeCart.getItems())) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " +
                                product.getName() + ". Available: " + product.getStockQuantity() + ", Requested: " + cartItem.getQuantity());
            }

            // Build snapshot OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSize(cartItem.getSize());
            orderItem.setSubtotal(
                    cartItem.getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP)
            );
            orderItemRepository.save(orderItem);

            // Deduct stock & remove cart item
            product.setStockQuantity(
                    product.getStockQuantity() - cartItem.getQuantity()
            );
            productService.saveProductEntity(product);

            activeCart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        }

        activeCart.setStatus(Cart.CartStatus.ORDERED);
        activeCart.setUser(userForOrder); // Ensure user is explicitly set if it was a guest cart being checked out by a logged-in user
        // The @PreUpdate in Cart entity will handle updatedAt, but saving explicitly ensures changes are flushed.
        cartRepository.save(activeCart);
        return order;
    }

    // ─── Private Helpers ──────────────────────────────────────────────────

    /**
     * Retrieves an active cart entity for the given user, or creates a new one.
     * This method is used internally by other service methods to ensure they always operate on an active cart.
     *
     * @param user The authenticated user.
     * @return An active Cart entity.
     * @throws IllegalArgumentException if user is null.
     */
    private Cart getCartEntityForAddOrUpdate(User user) {
        if (user != null && user.getId() != null) {
            // 1. Try to find an existing ACTIVE cart for the user
            return cartRepository.findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForUser(user));
        } else {
            // Critical: If neither user nor guestId is provided, we cannot identify the cart.
            throw new IllegalArgumentException("Cannot get or create cart: A user ID must be provided.");
        }
    }

    /**
     * Creates and saves a new active cart for a user.
     *
     * @param user The user for whom to create the cart.
     * @return The newly created Cart entity.
     */
    @Transactional
    private Cart createNewCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        logger.info("Creating new cart for user: {}", user.getId());
        // createdAt and updatedAt handled by @PrePersist in Cart entity
        return cartRepository.save(cart);
    }

    /**
     * Converts a Cart entity to a CartDto, including GST breakdown.
     *
     * @param cart The Cart entity to convert.
     * @return The CartDto with calculated totals and GST.
     */
    private CartDto convertToDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());

        BigDecimal subtotalBeforeTax = cart.getTotalPrice();
        BigDecimal gstAmount = subtotalBeforeTax.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotalBeforeTax.add(gstAmount);

        return new CartDto(
                cart.getId(),
                items,
                subtotalBeforeTax,
                gstAmount,
                totalAmount,
                cart.getTotalItems(),
                cart.getUser() != null ? cart.getUser().getId() : null,
                cart.getStatus().name()
        );
    }

    /**
     * Converts a CartItem entity to a CartItemDto.
     *
     * @param item The CartItem entity to convert.
     * @return The CartItemDto.
     */
    private CartItemDto convertItemToDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setUnitPrice(item.getPrice().doubleValue());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(
                item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .doubleValue()
        );
        dto.setImageUrl(item.getProduct().getImageUrl());
        dto.setSize(item.getSize());
        return dto;
    }
}