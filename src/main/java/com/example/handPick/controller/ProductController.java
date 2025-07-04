package com.example.handPick.controller;

import com.example.handPick.dto.ProductDto;
import com.example.handPick.dto.ProductUpdateDto;
import com.example.handPick.model.User; // Import User model
import com.example.handPick.service.ProductService;
import com.example.handPick.service.UserService; // Import UserService
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Import AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails; // Import UserDetails
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional; // Import Optional

@RestController
@RequestMapping("/api/products") // This controller handles product-related endpoints
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final UserService userService; // Inject UserService

    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService; // Initialize UserService
    }

    /**
     * Endpoint to retrieve all products.
     * Accessible by anyone.
     * GET /api/products
     * @return ResponseEntity with a list of ProductDto
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        logger.info("Fetching all products.");
        List<ProductDto> products = productService.findAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Endpoint to retrieve a single product by its ID.
     * Accessible by anyone. This serves as the backend for the product detail page.
     * GET /api/products/{id}
     * @param id The ID of the product to retrieve.
     * @return ResponseEntity with ProductDto if found, or 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        logger.info("Fetching product with ID: {}", id);
        return productService
                .getProductDtoById(id)
                .map(productDto -> {
                    logger.debug("Product found with ID: {}", id);
                    return ResponseEntity.ok(productDto);
                })
                .orElseGet(() -> {
                    logger.warn("Product not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint to create a new product.
     * Requires authenticated user with ID 1.
     * POST /api/products
     * @param productDto The DTO containing product details for creation.
     * @param userDetails Authenticated user details.
     * @return ResponseEntity with the created ProductDto and 201 Created status, or 403 Forbidden.
     */
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody ProductDto productDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.warn("Unauthorized attempt to create product: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication."));

        // Enforce restriction: Only user with ID 1 can create products
        if (currentUser.getId() != 1L) {
            logger.warn("Access Denied: User {} (ID: {}) attempted to create product.", currentUser.getUsername(), currentUser.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Attempting to create new product: {} by user ID: {}", productDto.getName(), currentUser.getId());
        productDto.setId(null); // Ensure ID is null for new product creation, as it's auto-generated
        try {
            ProductDto created = productService.saveProduct(productDto);
            logger.info("Product created successfully with ID: {}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            logger.error("Failed to create product {}: {}", productDto.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Or a more specific error DTO
        }
    }

    /**
     * Endpoint to update an existing product.
     * Requires authenticated user with ID 1.
     * PUT /api/products/{id}
     * @param id The ID of the product to update (from path variable).
     * @param productUpdateDto The DTO containing updated product details (from request body).
     * @param userDetails Authenticated user details.
     * @return ResponseEntity with the updated ProductDto, or 404 Not Found, or 403 Forbidden.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDto productUpdateDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.warn("Unauthorized attempt to update product: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication."));

        // Enforce restriction: Only user with ID 1 can update products
        if (currentUser.getId() != 1L) {
            logger.warn("Access Denied: User {} (ID: {}) attempted to update product with ID: {}", currentUser.getUsername(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Attempting to update product with ID: {} by user ID: {}", id, currentUser.getId());
        try {
            ProductDto updated = productService.updateProduct(id, productUpdateDto);
            logger.info("Product updated successfully with ID: {}", updated.getId());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("Failed to update product with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Return 404 if product not found
        }
    }

    /**
     * Endpoint to delete a product by its ID.
     * Requires authenticated user with ID 1.
     * DELETE /api/products/{id}
     * @param id The ID of the product to delete.
     * @param userDetails Authenticated user details.
     * @return ResponseEntity with 204 No Content status upon successful deletion, or 403 Forbidden.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.warn("Unauthorized attempt to delete product: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication."));

        // Enforce restriction: Only user with ID 1 can delete products
        if (currentUser.getId() != 1L) {
            logger.warn("Access Denied: User {} (ID: {}) attempted to delete product with ID: {}", currentUser.getUsername(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Attempting to delete product with ID: {} by user ID: {}", id, currentUser.getId());
        try {
            productService.deleteProduct(id);
            logger.info("Product deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build(); // 204 No Content indicates successful deletion
        } catch (RuntimeException e) {
            logger.error("Failed to delete product with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Or 400 Bad Request if ID is invalid
        }
    }
}