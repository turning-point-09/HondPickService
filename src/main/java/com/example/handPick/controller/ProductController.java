package com.example.handPick.controller;

import com.example.handPick.dto.ProductDto;
import com.example.handPick.dto.ProductUpdateDto;
import com.example.handPick.dto.ProductPageResponse;
import com.example.handPick.model.User;
import com.example.handPick.service.ProductService;
import com.example.handPick.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final UserService userService;

    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    /**
     * GET /api/v1/products?q={searchTerm}&page={page}&size={size}
     * GET /api/v1/products?q={searchTerm}
     * GET /api/v1/products
     * Retrieve all products or search by name/description with optional pagination.
     */
    @GetMapping
    public ResponseEntity<?> getAllProductsOrSearch(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "-1") int page,
            @RequestParam(defaultValue = "10") int size) {

        // If page is not specified (default -1), return all products without pagination
        if (page == -1) {
            if (q != null && !q.trim().isEmpty()) {
                logger.info("Searching products with term: {}", q);
                List<ProductDto> products = productService.searchProducts(q);
                return ResponseEntity.ok(products);
            } else {
                logger.info("Fetching all products.");
                List<ProductDto> products = productService.findAllProducts();
                return ResponseEntity.ok(products);
            }
        } else {
            Pageable pageable = PageRequest.of(page, size);
            logger.info("Fetching products with search term: '{}', page: {}, size: {}", q, page, size);
            
            // If search term is empty or null, get all products with pagination
            if (q == null || q.trim().isEmpty()) {
                Page<ProductDto> productsPage = productService.findAllProducts(pageable);
                ProductPageResponse response = new ProductPageResponse(
                    productsPage.getContent(),
                    productsPage.getTotalPages(),
                    productsPage.getTotalElements(),
                    productsPage.getNumber() + 1, // 1-based page number
                    productsPage.getSize()
                );
                return ResponseEntity.ok(response);
            } else {
                // Search with the provided term
                Page<ProductDto> productsPage = productService.searchProducts(q.trim(), pageable);
                ProductPageResponse response = new ProductPageResponse(
                    productsPage.getContent(),
                    productsPage.getTotalPages(),
                    productsPage.getTotalElements(),
                    productsPage.getNumber() + 1, // 1-based page number
                    productsPage.getSize()
                );
                return ResponseEntity.ok(response);
            }
        }
    }

    /**
     * GET /api/v1/products/{id}
     * Retrieve a single product by its ID.
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
     * POST /api/v1/products
     * Create a new product (admin only).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody ProductDto productDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.warn("Unauthorized attempt to create product: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        logger.info("Attempting to create new product: {} by user ID: {}", productDto.getName(), currentUser.getId());
        productDto.setId(null); // Ensure ID is null for new product creation
        try {
            ProductDto created = productService.saveProduct(productDto);
            logger.info("Product created successfully with ID: {}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.error("Failed to create product {}: {}", productDto.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * PUT /api/v1/products/{id}
     * Update an existing product (admin only).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDto productUpdateDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.warn("Unauthorized attempt to update product: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        logger.info("Attempting to update product with ID: {} by user ID: {}", id, currentUser.getId());
        try {
            ProductDto updated = productService.updateProduct(id, productUpdateDto);
            logger.info("Product updated successfully with ID: {}", updated.getId());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("Failed to update product with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("An unexpected error occurred while updating product with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/v1/products/{id}
     * Delete a product by its ID (admin only).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.warn("Unauthorized attempt to delete product: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        logger.info("Attempting to delete product with ID: {} by user ID: {}", id, currentUser.getId());
        try {
            productService.deleteProduct(id);
            logger.info("Product deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Failed to delete product with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("An unexpected error occurred while deleting product with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}