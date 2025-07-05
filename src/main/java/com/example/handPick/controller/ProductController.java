package com.example.handPick.controller;

import com.example.handPick.dto.ProductDto;
import com.example.handPick.dto.ProductUpdateDto;
import com.example.handPick.model.User;
import com.example.handPick.service.ProductService;
import com.example.handPick.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final UserService userService;

    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    /**
     * Endpoint to retrieve all products or search products by name/description.
     * Accessible by anyone.
     * GET /api/products?q={searchTerm}
     * GET /api/products
     * @param q Optional search term for name or description.
     * @return ResponseEntity with a list of ProductDto.
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProductsOrSearch(@RequestParam(required = false) String q) {
        if (q != null && !q.trim().isEmpty()) {
            logger.info("Searching products with term: {}", q);
            List<ProductDto> products = productService.searchProducts(q);
            return ResponseEntity.ok(products);
        } else {
            logger.info("Fetching all products.");
            List<ProductDto> products = productService.findAllProducts();
            return ResponseEntity.ok(products);
        }
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
     * Requires authenticated user with specific authorization (e.g., ADMIN role).
     * For demonstration, this example grants access to user with ID 1.
     * POST /api/products
     * @param productDto The DTO containing product details for creation.
     * @param userDetails Authenticated user details.
     * @return ResponseEntity with the created ProductDto and 201 Created status, or 403 Forbidden, or 401 Unauthorized.
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
                .orElseGet(() -> {
                    logger.error("Authenticated user '{}' not found in database. This indicates a configuration issue.", userDetails.getUsername());
                    // In a real app, this might be a 500 Internal Server Error or more specific handling.
                    throw new RuntimeException("Authenticated user not found.");
                });

        // IMPORTANT: This is a simplified, hardcoded authorization check for demonstration.
        // In a real application, use Spring Security's role-based or authority-based access control
        // (e.g., @PreAuthorize("hasRole('ADMIN')") or @PreAuthorize("hasAuthority('product:create')"))
        // and assign roles/authorities to users.
        if (currentUser.getId() != 1L) { // Changed condition: User ID 1 is now allowed to create.
            logger.warn("Access Denied: User {} (ID: {}) attempted to create product but is not authorized (only user ID 1 can for this demo).", currentUser.getUsername(), currentUser.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Attempting to create new product: {} by user ID: {}", productDto.getName(), currentUser.getId());
        productDto.setId(null); // Ensure ID is null for new product creation, as it's auto-generated
        try {
            ProductDto created = productService.saveProduct(productDto);
            logger.info("Product created successfully with ID: {}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) { // Catch more general Exception for robustness
            logger.error("Failed to create product {}: {}", productDto.getName(), e.getMessage(), e);
            // Consider custom exceptions in service layer for specific error types (e.g., InvalidProductDataException)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return 400 for validation/bad data issues
        }
    }

    /**
     * Endpoint to update an existing product.
     * Requires authenticated user with specific authorization (e.g., ADMIN role).
     * For demonstration, this example grants access to user with ID 1.
     * PUT /api/products/{id}
     * @param id The ID of the product to update (from path variable).
     * @param productUpdateDto The DTO containing updated product details (from request body).
     * @param userDetails Authenticated user details.
     * @return ResponseEntity with the updated ProductDto, or 404 Not Found, or 403 Forbidden, or 401 Unauthorized.
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
                .orElseGet(() -> {
                    logger.error("Authenticated user '{}' not found in database. This indicates a configuration issue.", userDetails.getUsername());
                    throw new RuntimeException("Authenticated user not found.");
                });

        // IMPORTANT: This is a simplified, hardcoded authorization check for demonstration.
        // In a real application, use Spring Security's role-based or authority-based access control.
        if (currentUser.getId() != 1L) { // Changed condition: User ID 1 is now allowed to update.
            logger.warn("Access Denied: User {} (ID: {}) attempted to update product with ID: {} but is not authorized (only user ID 1 can for this demo).", currentUser.getUsername(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Attempting to update product with ID: {} by user ID: {}", id, currentUser.getId());
        try {
            ProductDto updated = productService.updateProduct(id, productUpdateDto);
            logger.info("Product updated successfully with ID: {}", updated.getId());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) { // Catch the RuntimeException thrown by service for "not found"
            logger.error("Failed to update product with ID {}: {}", id, e.getMessage(), e);
            // Assuming RuntimeException from service means product not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) { // Catch other potential exceptions during update
            logger.error("An unexpected error occurred while updating product with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint to delete a product by its ID.
     * Requires authenticated user with specific authorization (e.g., ADMIN role).
     * For demonstration, this example grants access to user with ID 1.
     * DELETE /api/products/{id}
     * @param id The ID of the product to delete.
     * @param userDetails Authenticated user details.
     * @return ResponseEntity with 204 No Content status upon successful deletion, or 403 Forbidden, or 401 Unauthorized.
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
                .orElseGet(() -> {
                    logger.error("Authenticated user '{}' not found in database. This indicates a configuration issue.", userDetails.getUsername());
                    throw new RuntimeException("Authenticated user not found.");
                });

        // IMPORTANT: This is a simplified, hardcoded authorization check for demonstration.
        // In a real application, use Spring Security's role-based or authority-based access control.
        if (currentUser.getId() != 1L) { // Changed condition: User ID 1 is now allowed to delete.
            logger.warn("Access Denied: User {} (ID: {}) attempted to delete product with ID: {} but is not authorized (only user ID 1 can for this demo).", currentUser.getUsername(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Attempting to delete product with ID: {} by user ID: {}", id, currentUser.getId());
        try {
            productService.deleteProduct(id);
            logger.info("Product deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build(); // 204 No Content indicates successful deletion
        } catch (RuntimeException e) { // Catch the RuntimeException thrown by service for "not found"
            logger.error("Failed to delete product with ID {}: {}", id, e.getMessage(), e);
            // Assuming RuntimeException from service means product not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) { // Catch other potential exceptions during delete
            logger.error("An unexpected error occurred while deleting product with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}