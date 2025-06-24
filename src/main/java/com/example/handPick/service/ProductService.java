package com.example.handPick.service;

import com.example.handPick.dto.ProductDto; // Import ProductDto
import com.example.handPick.model.Product; // Import Product entity
import com.example.handPick.repository.ProductRepository; // Import ProductRepository
import org.springframework.beans.factory.annotation.Autowired; // For dependency injection
import org.springframework.stereotype.Service; // Marks this as a service component

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // For stream operations

@Service // Marks this class as a Spring service
public class ProductService {

    @Autowired // Injects the ProductRepository dependency
    private ProductRepository productRepository;

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
     * Retrieves a product by its ID and maps it to ProductDto.
     * @param id The ID of the product.
     * @return The ProductDto if found.
     * @throws RuntimeException if the product is not found.
     */
    public ProductDto findProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return convertToDto(product);
    }

    /**
     * Saves a new product or updates an existing one.
     * @param productDto The ProductDto to save.
     * @return The saved ProductDto.
     */
    public ProductDto saveProduct(ProductDto productDto) {
        Product product = convertToEntity(productDto);
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
        return productDto;
    }

    /**
     * Helper method to convert a ProductDto to a Product entity.
     * @param productDto The ProductDto.
     * @return The corresponding Product entity.
     */
    private Product convertToEntity(ProductDto productDto) {
        Product product = new Product();
        // ID should only be set for updates, not for new products (handled by DB auto-gen)
        if (productDto.getId() != null) {
            product.setId(productDto.getId());
        }
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setImageUrl(productDto.getImageUrl());
        product.setStockQuantity(productDto.getStockQuantity());
        return product;
    }
}