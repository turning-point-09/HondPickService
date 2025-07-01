package com.example.handPick.service;

import com.example.handPick.dto.AddToCartRequest;
import com.example.handPick.dto.CartDto;
import com.example.handPick.dto.CartItemDto;
import com.example.handPick.model.Cart;
import com.example.handPick.model.CartItem;
import com.example.handPick.model.Order;
import com.example.handPick.model.OrderItem;
import com.example.handPick.model.Product;
import com.example.handPick.model.User;
import com.example.handPick.repository.CartItemRepository;
import com.example.handPick.repository.CartRepository;
import com.example.handPick.repository.OrderItemRepository;
import com.example.handPick.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    @Autowired
    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       OrderRepository orderRepository,
                       OrderItemRepository orderItemRepository,
                       ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
    }

    @Transactional
    public CartDto getCart(User user, UUID guestId) {
        Cart cart;
        if (user != null) {
            cart = cartRepository
                    .findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForUser(user));
        } else if (guestId != null) {
            cart = cartRepository
                    .findByGuestIdAndStatus(guestId, Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForGuest(guestId));
        } else {
            cart = createNewCartForGuest(UUID.randomUUID());
        }
        return convertToDto(cart);
    }

    @Transactional
    public CartDto addItemToCart(User user, UUID guestId, AddToCartRequest request) {
        if (request.getProductId() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Invalid product ID or quantity.");
        }

        Cart cart = getCartEntityForAddOrUpdate(user, guestId);
        Product product = productService.getProductEntityById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + request.getProductId()));

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst();

        int addQty = request.getQuantity();
        int currentQty = existing.map(CartItem::getQuantity).orElse(0);
        int newTotal = currentQty + addQty;

        if (product.getStockQuantity() != null && product.getStockQuantity() < newTotal) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " +
                            product.getStockQuantity() + ", requested: " + newTotal);
        }

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(newTotal);
            item.setPrice(product.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem(product, addQty, cart);
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        // Deduct stock
        if (product.getStockQuantity() != null) {
            product.setStockQuantity(product.getStockQuantity() - addQty);
            productService.saveProductEntity(product);
        }

        cartRepository.save(cart);
        return convertToDto(cart);
    }

    @Transactional
    public CartDto updateItemQuantity(User user, UUID guestId,
                                      Long productId, int quantity) {
        Cart cart = getCartEntityForAddOrUpdate(user, guestId);
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
            if (product.getStockQuantity() != null) {
                product.setStockQuantity(product.getStockQuantity() + oldQty);
                productService.saveProductEntity(product);
            }
        } else {
            if (diff > 0
                    && product.getStockQuantity() != null
                    && product.getStockQuantity() < diff) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Only " +
                                product.getStockQuantity() +
                                " more units available."
                );
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            if (product.getStockQuantity() != null) {
                product.setStockQuantity(product.getStockQuantity() - diff);
                productService.saveProductEntity(product);
            }
        }

        cartRepository.save(cart);
        return convertToDto(cart);
    }

    @Transactional
    public CartDto removeItemFromCart(User user, UUID guestId, Long productId) {
        Cart cart = getCartEntityForAddOrUpdate(user, guestId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not in cart: " + productId));

        Product product = item.getProduct();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        if (product.getStockQuantity() != null) {
            product.setStockQuantity(
                    product.getStockQuantity() + item.getQuantity());
            productService.saveProductEntity(product);
        }

        cartRepository.save(cart);
        return convertToDto(cart);
    }

    @Transactional
    public void mergeCarts(User user, UUID guestId) {
        if (user == null || guestId == null) {
            throw new IllegalArgumentException(
                    "Both user and guestId required for merge.");
        }

        Optional<Cart> userCartOpt =
                cartRepository.findByUserAndStatus(user, Cart.CartStatus.ACTIVE);
        Optional<Cart> guestCartOpt =
                cartRepository.findByGuestIdAndStatus(guestId, Cart.CartStatus.ACTIVE);

        if (guestCartOpt.isEmpty()) {
            return;
        }

        Cart guestCart = guestCartOpt.get();
        Cart userCart = userCartOpt
                .orElseGet(() -> createNewCartForUser(user));

        for (CartItem guestItem : new ArrayList<>(guestCart.getItems())) {
            Product product = guestItem.getProduct();
            int combined = userCart.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(product.getId()))
                    .mapToInt(CartItem::getQuantity)
                    .sum() + guestItem.getQuantity();

            if (product.getStockQuantity() != null
                    && product.getStockQuantity() < combined) {
                continue;
            }

            Optional<CartItem> existing =
                    userCart.getItems().stream()
                            .filter(i -> i.getProduct().getId().equals(product.getId()))
                            .findFirst();

            if (existing.isPresent()) {
                CartItem existingItem = existing.get();
                existingItem.setQuantity(combined);
                existingItem.setPrice(product.getPrice());
                cartItemRepository.save(existingItem);
            } else {
                guestItem.setCart(userCart);
                cartItemRepository.save(guestItem);
                userCart.getItems().add(guestItem);
            }

            guestCart.getItems().remove(guestItem);
            cartItemRepository.delete(guestItem);
        }

        guestCart.setStatus(Cart.CartStatus.ABANDONED);
        cartRepository.save(guestCart);
        cartRepository.save(userCart);
    }

    @Transactional
    public Order checkoutCart(User user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "Authenticated user required for checkout.");
        }

        Cart activeCart = cartRepository
                .findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No active cart for user: " + user.getUsername())
                );

        if (activeCart.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot checkout an empty cart.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(activeCart.getTotalPrice());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order = orderRepository.save(order);

        for (CartItem cartItem : new ArrayList<>(activeCart.getItems())) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " +
                                product.getName());
            }

            // Build snapshot OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(
                    cartItem.getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
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
        cartRepository.save(activeCart);
        return order;
    }

    // ─── Private Helpers ──────────────────────────────────────────────────

    private Cart getCartEntityForAddOrUpdate(User user, UUID guestId) {
        if (user != null) {
            return cartRepository
                    .findByUserAndStatus(user, Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForUser(user));
        } else if (guestId != null) {
            return cartRepository
                    .findByGuestIdAndStatus(guestId, Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForGuest(guestId));
        } else {
            return createNewCartForGuest(UUID.randomUUID());
        }
    }

    private Cart createNewCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private Cart createNewCartForGuest(UUID guestId) {
        Cart cart = new Cart();
        cart.setGuestId(guestId);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private CartDto convertToDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());

        return new CartDto(
                cart.getId(),
                items,
                cart.getTotalPrice(),
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
        dto.setUnitPrice(item.getPrice().doubleValue());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(
                item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .doubleValue()
        );
        dto.setImageUrl(item.getProduct().getImageUrl());
        return dto;
    }
}
