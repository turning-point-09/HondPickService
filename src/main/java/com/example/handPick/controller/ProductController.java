package com.example.handPick.controller;

import com.example.handPick.dto.ProductDto; // Import ProductDto
import com.example.handPick.service.ProductService; // Import ProductService
import jakarta.validation.Valid; // For DTO validation
import org.springframework.http.HttpStatus; // HTTP status codes
import org.springframework.http.ResponseEntity; // Wrapper for HTTP response
import org.springframework.security.access.prepost.PreAuthorize; // For security
import org.springframework.web.bind.annotation.*; // REST annotations

import java.util.List;

@RestController // Marks this class as a REST controller
@RequestMapping("/api/products") // Base path for all endpoints in this controller
public class ProductController {

    // Using constructor injection is generally preferred over @Autowired on fields
    private final ProductService productService;

    // Constructor Injection
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/products
     * Retrieves a list of all products. This endpoint is generally public.
     * @return ResponseEntity containing a list of ProductDto and HTTP status OK.
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.findAllProducts();
        return ResponseEntity.ok(products); // Returns 200 OK with the list of products
    }

    /**
     * GET /api/products/{id}
     * Retrieves a single product by its ID and maps it to ProductDto.
     * This endpoint is generally public.
     * @param id The ID of the product to retrieve.
     * @return ResponseEntity containing the ProductDto and HTTP status OK if found,
     * or HTTP status NOT_FOUND if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        // Using Optional from service for cleaner error handling
        return productService.getProductById(id) // This method now returns Optional<ProductDto> from Service
                .map(ResponseEntity::ok) // If present, return 200 OK
                .orElse(ResponseEntity.notFound().build()); // If not present, return 404 Not Found
    }

    /**
     * POST /api/products
     * Creates a new product.
     * Accessible only to authenticated users.
     * @param productDto The ProductDto containing the new product's details.
     * @return ResponseEntity containing the created ProductDto and HTTP status CREATED.
     */
    @PreAuthorize("isAuthenticated()") // Ensure only authenticated users can create products
    @PostMapping // Handles POST requests to /api/products
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) { // Added @Valid
        // IMPORTANT: Ensure ID is null for new products so the database auto-generates it.
        productDto.setId(null);
        ProductDto createdProduct = productService.saveProduct(productDto); // Using the unified saveProduct
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct); // Returns 201 Created
    }

    /**
     * PUT /api/products/{id}
     * Updates an existing product.
     * Accessible only to authenticated users.
     * @param id The ID of the product to update.
     * @param productDto The ProductDto with updated details.
     * @return ResponseEntity containing the updated ProductDto and HTTP status OK.
     * Returns HTTP status NOT_FOUND if the product does not exist.
     */
    @PreAuthorize("isAuthenticated()") // Ensure only authenticated users can update products
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto productDto) { // Added @Valid
        try {
            // Fix: Set the ID on a separate line, then pass the modified DTO to the service.
            productDto.setId(id); // Set the ID from the path to the DTO

            ProductDto updatedProduct = productService.saveProduct(productDto); // Pass the DTO itself

            return ResponseEntity.ok(updatedProduct); // Returns 200 OK with the updated product
        } catch (RuntimeException e) {
            // This catches "Product not found" from the service
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Returns 404 Not Found
        }
    }

    /**
     * DELETE /api/products/{id}
     * Deletes a product by its ID.
     * Accessible only to authenticated users.
     * @param id The ID of the product to delete.
     * @return ResponseEntity with HTTP status NO_CONTENT.
     */
    @PreAuthorize("isAuthenticated()") // Ensure only authenticated users can delete products
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // Returns 204 No Content
    }
}