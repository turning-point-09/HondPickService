package com.example.handPick.service;

import com.example.handPick.dto.AddToCartRequest;
import com.example.handPick.dto.CartDto;
import com.example.handPick.dto.CartItemDto;
import com.example.handPick.model.Cart;
import com.example.handPick.model.Cart.CartStatus;
import com.example.handPick.model.CartItem;
import com.example.handPick.model.Order; // NEW IMPORT
import com.example.handPick.model.OrderItem; // NEW IMPORT
import com.example.handPick.model.User;
import com.example.handPick.repository.CartItemRepository;
import com.example.handPick.repository.CartRepository;
import com.example.handPick.repository.OrderItemRepository; // NEW IMPORT
import com.example.handPick.repository.OrderRepository; // NEW IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired // NEW
    private OrderRepository orderRepository;

    @Autowired // NEW
    private OrderItemRepository orderItemRepository;

    @Transactional
    public CartDto getCart(User user, UUID guestId) {
        Cart cart;
        if (user != null) {
            cart = cartRepository.findByUserAndStatus(user, CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForUser(user));
        } else if (guestId != null) {
            cart = cartRepository.findByGuestIdAndStatus(guestId, CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForGuest(guestId));
        } else {
            throw new IllegalArgumentException("Neither user nor guest ID provided for cart retrieval.");
        }
        return convertToDto(cart);
    }

    @Transactional
    public CartDto addItemToCart(User user, UUID guestId, AddToCartRequest request) {
        if (request.getProductId() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Invalid product ID or quantity.");
        }

        Cart cart;
        if (user != null) {
            cart = cartRepository.findByUserAndStatus(user, CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForUser(user));
        } else if (guestId != null) {
            cart = cartRepository.findByGuestIdAndStatus(guestId, CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCartForGuest(guestId));
        } else {
            throw new IllegalArgumentException("Cannot add to cart without a user or guest ID.");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductId(cart, request.getProductId());

        BigDecimal productUnitPrice = getProductPrice(request.getProductId()); // Simulate fetching product price

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUnitPrice(productUnitPrice); // Update price in case it changed
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setProductName("Product " + request.getProductId()); // Placeholder
            newItem.setQuantity(request.getQuantity());
            newItem.setUnitPrice(productUnitPrice);
            cart.getItems().add(newItem); // Add to list to maintain relationship
            cartItemRepository.save(newItem);
        }

        updateCartTotals(cart);
        return convertToDto(cart);
    }

    @Transactional
    public void mergeCarts(User user, UUID guestId) {
        Optional<Cart> userCartOpt = cartRepository.findByUserAndStatus(user, CartStatus.ACTIVE);
        Optional<Cart> guestCartOpt = cartRepository.findByGuestIdAndStatus(guestId, CartStatus.ACTIVE);

        if (guestCartOpt.isPresent()) {
            Cart guestCart = guestCartOpt.get();
            Cart userCart = userCartOpt.orElseGet(() -> createNewCartForUser(user));

            for (CartItem guestItem : guestCart.getItems()) {
                Optional<CartItem> existingUserItemOpt = cartItemRepository.findByCartAndProductId(userCart, guestItem.getProductId());

                if (existingUserItemOpt.isPresent()) {
                    CartItem userItem = existingUserItemOpt.get();
                    userItem.setQuantity(userItem.getQuantity() + guestItem.getQuantity());
                    userItem.setUnitPrice(guestItem.getUnitPrice()); // Update to guest item's price or current product price
                    cartItemRepository.save(userItem);
                } else {
                    // Add guest item to user's cart
                    guestItem.setCart(userCart); // Re-assign cart
                    cartItemRepository.save(guestItem); // Save to link it to the user's cart
                }
            }

            guestCart.setStatus(CartStatus.ABANDONED); // Mark guest cart as abandoned
            cartRepository.save(guestCart); // Save status change
            updateCartTotals(userCart); // Recalculate user cart total
        }
    }

    // NEW CHECKOUT METHOD
    @Transactional
    public Order checkoutCart(User user) {
        // Find the user's active cart
        Cart activeCart = cartRepository.findByUserAndStatus(user, CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active cart found for user: " + user.getUsername()));

        if (activeCart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout an empty cart.");
        }

        // 1. Create a new Order
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(activeCart.getTotalPrice());
        order.setStatus(Order.OrderStatus.PENDING); // Initial status

        order = orderRepository.save(order); // Save order to get its ID

        // 2. Move cart items to order items
        for (CartItem cartItem : activeCart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(cartItem.getSubtotal()); // Ensure subtotal is calculated on cartItem

            order.getItems().add(orderItem); // Add to order's list
            orderItemRepository.save(orderItem); // Save the order item
            // Optional: Deduct stock here if you have a product inventory system
        }

        // 3. Update the cart status to ORDERED
        activeCart.setStatus(CartStatus.ORDERED);
        cartRepository.save(activeCart);

        // 4. Optionally, you might want to create a new empty active cart for the user
        //    Or simply let the `getCart` method create one on next request.
        //    For now, we'll let getCart handle it.

        return order; // Return the created order
    }

    private Cart createNewCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private Cart createNewCartForGuest(UUID guestId) {
        Cart cart = new Cart();
        cart.setGuestId(guestId);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            item.calculateSubtotal(); // Ensure subtotal is calculated
            total = total.add(item.getSubtotal());
        }
        cart.setTotalPrice(total);
        cartRepository.save(cart); // Save updated totals
    }

    // Dummy method to simulate fetching product price
    private BigDecimal getProductPrice(Long productId) {
        // In a real application, you'd fetch this from a ProductService or ProductRepository
        if (productId.equals(1L)) return new BigDecimal("10.00");
        if (productId.equals(2L)) return new BigDecimal("25.50");
        return new BigDecimal("5.00"); // Default price
    }

    private CartDto convertToDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setStatus(cart.getStatus().name());
        dto.setTotalPrice(cart.getTotalPrice());
        dto.setItems(cart.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private CartItemDto convertItemToDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        item.calculateSubtotal(); // Ensure subtotal is calculated before setting
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }
}