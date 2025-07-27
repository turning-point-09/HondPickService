package com.example.handPick.service;

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
import com.example.handPick.service.UserAddressService;

@Service
public class CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final UserService userService;
    private final UserAddressService userAddressService;
    private final NotificationService notificationService;

    @Value("${app.gst.rate:0.18}")
    private double gstRate;

    @Autowired
    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       OrderRepository orderRepository,
                       OrderItemRepository orderItemRepository,
                       ProductService productService,
                       UserService userService,
                       UserAddressService userAddressService,
                       NotificationService notificationService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
        this.userService = userService;
        this.userAddressService = userAddressService;
        this.notificationService = notificationService;
    }

    /**
     * Retrieves the active cart for a given user.
     * If no active cart is found, a new one is created.
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
        int availableStock = getAvailableStock(product);
        int totalInCart = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .mapToInt(CartItem::getQuantity)
                .sum();
        
        int totalRequested = totalInCart + addQty;
        
        if (availableStock < totalRequested) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + availableStock + 
                    ", requested: " + totalRequested + 
                    " (already in cart: " + totalInCart + ")");
        }

        if (existing.isPresent()) {
            // Update quantity of existing item
            CartItem item = existing.get();
            item.setQuantity(newTotal);
            item.setPrice(product.getPrice());
            cartItemRepository.save(item);
        } else {
            // Add new item to cart
            CartItem item = new CartItem(product, addQty, cart, request.getSize());
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        // DO NOT deduct stock here - stock is only deducted during checkout

        cartRepository.save(cart);
        return convertToDto(cart);
    }

    /**
     * Updates the quantity of a specific item in the cart.
     * If quantity is 0 or less, the item is removed.
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
        int diff = quantity - oldQty;

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
            // DO NOT add stock back here - stock is only managed during checkout
        } else {
            // Check if new quantity exceeds available stock
            int availableStock = getAvailableStock(product);
            int totalInCart = cart.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(productId))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            
            int otherItemsInCart = totalInCart - oldQty; // Items of same product in cart, excluding current item
            int totalRequested = otherItemsInCart + quantity;
            
            if (availableStock < totalRequested) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available: " + availableStock + 
                        ", requested: " + totalRequested + 
                        " (other items in cart: " + otherItemsInCart + ")");
            }
            
            item.setQuantity(quantity);
            item.setPrice(product.getPrice());
            cartItemRepository.save(item);
            // DO NOT deduct stock here - stock is only deducted during checkout
        }

        cartRepository.save(cart);
        return convertToDto(cart);
    }

    /**
     * Calculates available stock for a product considering items in active carts
     */
    private int getAvailableStock(Product product) {
        if (product.getStockQuantity() == null) {
            return 0;
        }
        
        // Get total quantity of this product in all active carts
        int totalInCarts = cartItemRepository.findAll().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .filter(item -> item.getCart().getStatus() == Cart.CartStatus.ACTIVE)
                .mapToInt(CartItem::getQuantity)
                .sum();
        
        return product.getStockQuantity() - totalInCarts;
    }

    /**
     * Removes a specific item from the cart.
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
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        // DO NOT add stock back here - stock is only managed during checkout

        cartRepository.save(cart);
        return convertToDto(cart);
    }

    /**
     * Checks out the active cart for an authenticated user, creating an order.
     */
    @Transactional
    public Order checkoutCart(User initialUser, CheckoutRequest checkoutRequest) {
        if (initialUser == null) {
            throw new IllegalArgumentException(
                    "Authenticated user required for checkout.");
        }

        User userForOrder = userService.findById(initialUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found during checkout."));

        // Handle address selection and management
        ShippingAddress orderShippingAddress = handleAddressForCheckout(userForOrder, checkoutRequest);

        Cart activeCart = cartRepository
                .findByUserAndStatus(userForOrder, Cart.CartStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No active cart for user, or cart is not in ACTIVE status."));

        if (activeCart.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot checkout an empty cart.");
        }

        BigDecimal subtotalBeforeTax = activeCart.getTotalPrice();
        BigDecimal gstAmount = subtotalBeforeTax.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotalBeforeTax.add(gstAmount);

        Order order = new Order();
        order.setUser(userForOrder);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(checkoutRequest.getPaymentMethod());
        order.setShippingAddress(orderShippingAddress);

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

        for (CartItem cartItem : new ArrayList<>(activeCart.getItems())) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " +
                                product.getName() + ". Available: " + product.getStockQuantity() + ", Requested: " + cartItem.getQuantity());
            }

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

            product.setStockQuantity(
                    product.getStockQuantity() - cartItem.getQuantity()
            );
            productService.saveProductEntity(product);

            activeCart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        }

        activeCart.setStatus(Cart.CartStatus.ORDERED);
        activeCart.setUser(userForOrder);
        cartRepository.save(activeCart);
        
        // Notify admin about new order
        notificationService.notifyNewOrder(order, userForOrder);
        
        return order;
    }

    /**
     * Handle address selection and management during checkout
     */
    private ShippingAddress handleAddressForCheckout(User user, CheckoutRequest checkoutRequest) {
        ShippingAddress orderShippingAddress = new ShippingAddress();

        // If user selected an existing address
        if (checkoutRequest.getSelectedAddressId() != null) {
            com.example.handPick.dto.UserAddressDto selectedAddress = userAddressService.getAddressById(
                    checkoutRequest.getSelectedAddressId(), user.getId());
            
            orderShippingAddress.setStreet(selectedAddress.getStreet());
            orderShippingAddress.setCity(selectedAddress.getCity());
            orderShippingAddress.setState(selectedAddress.getState());
            orderShippingAddress.setPostalCode(selectedAddress.getPostalCode());
            orderShippingAddress.setCountry(selectedAddress.getCountry());
        }
        // If user provided a new address
        else if (checkoutRequest.getNewAddress() != null) {
            com.example.handPick.dto.AddressDto newAddress = checkoutRequest.getNewAddress();
            
            orderShippingAddress.setStreet(newAddress.getStreet());
            orderShippingAddress.setCity(newAddress.getCity());
            orderShippingAddress.setState(newAddress.getState());
            orderShippingAddress.setPostalCode(newAddress.getPostalCode());
            orderShippingAddress.setCountry(newAddress.getCountry());

            // Handle address saving based on user preference
            if (checkoutRequest.getSaveAddressAs() != null && 
                !checkoutRequest.getSaveAddressAs().equals("ONE_TIME")) {
                
                com.example.handPick.dto.UserAddressDto addressToSave = new com.example.handPick.dto.UserAddressDto();
                addressToSave.setStreet(newAddress.getStreet());
                addressToSave.setCity(newAddress.getCity());
                addressToSave.setState(newAddress.getState());
                addressToSave.setPostalCode(newAddress.getPostalCode());
                addressToSave.setCountry(newAddress.getCountry());
                addressToSave.setAddressLabel(checkoutRequest.getAddressLabel());
                addressToSave.setAddressType(checkoutRequest.getSaveAddressAs());
                addressToSave.setDefault(checkoutRequest.isSetAsDefault());

                userAddressService.addAddress(user.getId(), addressToSave);
                logger.info("Saved new address for user {} as {}", user.getId(), checkoutRequest.getSaveAddressAs());
            }
        }
        // Use default address if available
        else {
            return userAddressService.getDefaultAddress(user.getId())
                    .map(defaultAddress -> {
                        ShippingAddress defaultShippingAddress = new ShippingAddress();
                        defaultShippingAddress.setStreet(defaultAddress.getStreet());
                        defaultShippingAddress.setCity(defaultAddress.getCity());
                        defaultShippingAddress.setState(defaultAddress.getState());
                        defaultShippingAddress.setPostalCode(defaultAddress.getPostalCode());
                        defaultShippingAddress.setCountry(defaultAddress.getCountry());
                        return defaultShippingAddress;
                    })
                    .orElseThrow(() -> new IllegalArgumentException("No shipping address provided and no default address found."));
        }

        return orderShippingAddress;
    }

    // ─── Private Helpers ──────────────────────────────────────────────────

    private Cart getCartEntityForAddOrUpdate(User user) {
        if (user != null && user.getId() != null) {
            return cartRepository.findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForUser(user));
        } else {
            throw new IllegalArgumentException("Cannot get or create cart: A user ID must be provided.");
        }
    }

    @Transactional
    private Cart createNewCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        logger.info("Creating new cart for user: {}", user.getId());
        return cartRepository.save(cart);
    }

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

    private CartItemDto convertItemToDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setUnitPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(
                item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
        );
        dto.setImageUrl(item.getProduct().getImageUrl());
        dto.setSize(item.getSize());
        return dto;
    }
}