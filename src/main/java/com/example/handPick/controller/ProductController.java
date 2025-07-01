package com.example.handPick.controller;

import com.example.handPick.dto.ProductDto;
import com.example.handPick.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Endpoint to retrieve all products.
     * Accessible by anyone.
     * GET /api/products
     * @return ResponseEntity with a list of ProductDto
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.findAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Endpoint to retrieve a single product by its ID.
     * Accessible by anyone.
     * GET /api/products/{id}
     * @param id The ID of the product to retrieve.
     * @return ResponseEntity with ProductDto if found, or 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productService
                .getProductDtoById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Endpoint to create a new product.
     * Requires authenticated user. Consider changing to hasRole('ADMIN') for production.
     * POST /api/products
     * @param productDto The DTO containing product details for creation.
     * @return ResponseEntity with the created ProductDto and 201 Created status.
     */
    @PreAuthorize("isAuthenticated()") // Only authenticated users can create products
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        productDto.setId(null); // Ensure ID is null for new product creation, as it's auto-generated
        ProductDto created = productService.saveProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Endpoint to update an existing product.
     * Requires authenticated user. Consider changing to hasRole('ADMIN') for production.
     * PUT /api/products/{id}
     * @param id The ID of the product to update (from path variable).
     * @param productDto The DTO containing updated product details (from request body).
     * @return ResponseEntity with the updated ProductDto, or 404 Not Found if product doesn't exist.
     */
    @PreAuthorize("isAuthenticated()") // Only authenticated users can update products
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id,
                                                    @Valid @RequestBody ProductDto productDto) {
        productDto.setId(id); // Ensure the ID from path variable is set in the DTO for service layer
        try {
            ProductDto updated = productService.saveProduct(productDto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            // In a real application, you might log the exception (e.g., logger.error("Product update failed", e);)
            // and provide a more specific error message.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Return 404 if product not found
        }
    }

    /**
     * Endpoint to delete a product by its ID.
     * Requires authenticated user. Consider changing to hasRole('ADMIN') for production.
     * DELETE /api/products/{id}
     * @param id The ID of the product to delete.
     * @return ResponseEntity with 204 No Content status upon successful deletion.
     */
    @PreAuthorize("isAuthenticated()") // Only authenticated users can delete products
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        // You might want to add a check here to ensure the product exists before attempting deletion
        // For example: productService.getProductEntityById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // 204 No Content indicates successful deletion
    }
}
