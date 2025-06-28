package com.example.handPick.service;

import com.example.handPick.dto.ProductDto; // Import ProductDto
import com.example.handPick.model.Product; // Import Product entity
import com.example.handPick.repository.ProductRepository; // Import ProductRepository
import org.springframework.beans.factory.annotation.Autowired; // For dependency injection
import org.springframework.stereotype.Service; // Marks this as a service component
import org.springframework.transaction.annotation.Transactional; // For transactional methods

import java.util.List;
import java.util.Optional; // Make sure this is imported
import java.util.stream.Collectors; // For stream operations

@Service // Marks this class as a Spring service
public class ProductService {

    // It's good practice to use constructor injection instead of field injection.
    // private ProductRepository productRepository; // Original field injection
    private final ProductRepository productRepository; // Change to final for constructor injection

    // Use constructor injection
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Retrieves all products and maps them to ProductDto.
     * @return A list of ProductDto.
     */
    public List<ProductDto> findAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDto) // Map each Product entity to a ProductDto
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a product by its ID and maps it to an Optional ProductDto.
     * This method is MODIFIED to return Optional<ProductDto>
     * @param id The ID of the product.
     * @return An Optional containing the ProductDto if found, otherwise an empty Optional.
     */
    public Optional<ProductDto> getProductById(Long id) { // MODIFIED method name and return type
        return productRepository.findById(id) // findById already returns Optional<Product>
                .map(this::convertToDto); // Map the Optional<Product> to Optional<ProductDto>
    }

    /**
     * Saves a new product or updates an existing one.
     * This method now includes logic to handle consistency between oldPrice and discountPercentage.
     * @param productDto The ProductDto to save.
     * @return The saved ProductDto.
     */
    @Transactional // Ensures the operation is atomic
    public ProductDto saveProduct(ProductDto productDto) {
        Product product;
        if (productDto.getId() != null) {
            // If ID is present, it's an update operation
            product = productRepository.findById(productDto.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productDto.getId() + " for update."));
            // Update existing fields from DTO
            product.setName(productDto.getName());
            product.setDescription(productDto.getDescription());
            product.setPrice(productDto.getPrice());
            product.setImageUrl(productDto.getImageUrl());
            product.setStockQuantity(productDto.getStockQuantity());
            product.setRating(productDto.getRating());
            product.setOldPrice(productDto.getOldPrice());
            product.setDiscountPercentage(productDto.getDiscountPercentage());
        } else {
            // If ID is null, it's a new product creation
            product = convertToEntity(productDto); // Use convertToEntity for initial mapping of new product
        }

        // --- Consistency Logic for oldPrice and discountPercentage ---
        // This logic applies to both new product creation and existing product updates

        Double price = product.getPrice();
        Double oldPrice = product.getOldPrice();
        Double discountPercentage = product.getDiscountPercentage();

        // Case 1: oldPrice provided, discountPercentage is null or zero
        if (oldPrice != null && oldPrice > 0 && (discountPercentage == null || discountPercentage == 0.0)) {
            if (oldPrice > price) {
                // Calculate discountPercentage based on oldPrice and current price
                double calculatedDiscount = ((oldPrice - price) / oldPrice) * 100.0;
                product.setDiscountPercentage(Math.round(calculatedDiscount * 100.0) / 100.0); // Round to 2 decimal places
            } else {
                // If oldPrice is not greater than current price, no discount
                product.setDiscountPercentage(0.0);
            }
        }
        // Case 2: discountPercentage provided, oldPrice is null or zero
        else if (discountPercentage != null && discountPercentage > 0 && discountPercentage <= 100 && (oldPrice == null || oldPrice == 0.0)) {
            // Calculate oldPrice based on discountPercentage and current price
            double calculatedOldPrice = price / (1 - discountPercentage / 100.0);
            product.setOldPrice(Math.round(calculatedOldPrice * 100.0) / 100.0); // Round to 2 decimal places
        }
        // Case 3: Both provided, check for inconsistency (optional but good practice)
        else if (oldPrice != null && oldPrice > 0 && discountPercentage != null && discountPercentage > 0 && discountPercentage <= 100) {
            // Calculate expected discount based on provided oldPrice and current price
            double calculatedDiscountFromOldPrice = ((oldPrice - price) / oldPrice) * 100.0;
            // Check if the provided discountPercentage is significantly different from the calculated one
            if (Math.abs(calculatedDiscountFromOldPrice - discountPercentage) > 0.01) { // Tolerance for floating point
                System.out.println("Warning: Inconsistency detected between oldPrice and discountPercentage for product: " + product.getName());
                System.out.println("Provided Old Price: " + oldPrice + ", Provided Discount: " + discountPercentage + "%, Calculated Discount: " + calculatedDiscountFromOldPrice + "%");
                // You might choose to:
                // a) Prioritize one (e.g., product.setDiscountPercentage(calculatedDiscountFromOldPrice);)
                // b) Throw an error
                // For now, we'll log a warning and use the values as provided from the DTO.
            }
        }
        // If neither is provided, or discount is 0/invalid, ensure both are null/0
        else if ((oldPrice == null || oldPrice == 0.0) && (discountPercentage == null || discountPercentage == 0.0)) {
            product.setOldPrice(null);
            product.setDiscountPercentage(null);
        }


        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }

    /**
     * Deletes a product by its ID.
     * @param id The ID of the product to delete.
     */
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    /**
     * Helper method to convert a Product entity to a ProductDto.
     * Maps all fields including new ones.
     * @param product The Product entity.
     * @return The corresponding ProductDto.
     */
    private ProductDto convertToDto(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId());
        productDto.setName(product.getName());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setImageUrl(product.getImageUrl());
        productDto.setStockQuantity(product.getStockQuantity());
        productDto.setRating(product.getRating()); // Map rating
        productDto.setOldPrice(product.getOldPrice()); // Map oldPrice
        productDto.setDiscountPercentage(product.getDiscountPercentage()); // Map discountPercentage
        return productDto;
    }

    /**
     * Helper method to convert a ProductDto to a Product entity.
     * Maps all fields including new ones.
     * @param productDto The ProductDto.
     * @return The corresponding Product entity.
     */
    private Product convertToEntity(ProductDto productDto) {
        Product product = new Product();
        // ID should only be set for updates, not for new products (handled by DB auto-gen)
        // For new products, productDto.getId() will be null, and it will be assigned by DB
        product.setId(productDto.getId());

        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setImageUrl(productDto.getImageUrl());
        product.setStockQuantity(productDto.getStockQuantity());
        product.setRating(productDto.getRating()); // Map rating
        product.setOldPrice(productDto.getOldPrice()); // Map oldPrice
        product.setDiscountPercentage(productDto.getDiscountPercentage()); // Map discountPercentage
        return product;
    }
}