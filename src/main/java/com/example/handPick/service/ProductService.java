package com.example.handPick.service;

import com.example.handPick.dto.ProductDto;
import com.example.handPick.dto.ProductUpdateDto;
import com.example.handPick.model.Product;
import com.example.handPick.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Finds all products and converts them to DTOs.
     * @return A list of ProductDto.
     */
    public List<ProductDto> findAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Finds all products with pagination and converts them to DTOs.
     * @param pageable The pagination parameters.
     * @return A Page of ProductDto.
     */
    public Page<ProductDto> findAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    /**
     * Finds a product by its ID and converts it to a DTO.
     * @param id The ID of the product.
     * @return An Optional containing the ProductDto if found.
     */
    public Optional<ProductDto> getProductDtoById(Long id) {
        return productRepository.findById(id).map(this::convertToDto);
    }

    /**
     * Finds a product entity by its ID.
     * @param id The ID of the product.
     * @return An Optional containing the Product entity if found.
     */
    public Optional<Product> getProductEntityById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Saves a product (either creates a new one or updates an existing one).
     * @param productDto The ProductDto to save.
     * @return The saved ProductDto.
     * @throws RuntimeException if product not found for update.
     */
    @Transactional
    public ProductDto saveProduct(ProductDto productDto) {
        Product product;
        if (productDto.getId() == null) {
            // Create new product
            product = new Product();
        } else {
            // Update existing product
            product = productRepository.findById(productDto.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productDto.getId()));
        }

        // Map fields from DTO to entity
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setPurchasePrice(productDto.getPurchasePrice());
        product.setImageUrl(productDto.getImageUrl());
        product.setStockQuantity(productDto.getStockQuantity());
        product.setRating(productDto.getRating());
        product.setOldPrice(productDto.getOldPrice());
        product.setDiscountPercentage(productDto.getDiscountPercentage());
        product.setSizeOptions(productDto.getSizeOptions());
        product.setCategory(productDto.getCategory());

        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }

    /**
     * Updates an existing product with data from the ProductUpdateDto.
     * @param id The ID of the product to update.
     * @param updateDto The ProductUpdateDto containing the updated data.
     * @return The updated ProductDto.
     * @throws RuntimeException if the product is not found.
     */
    @Transactional
    public ProductDto updateProduct(Long id, ProductUpdateDto updateDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        if (updateDto.getName() != null) {
            product.setName(updateDto.getName());
        }
        if (updateDto.getDescription() != null) {
            product.setDescription(updateDto.getDescription());
        }
        if (updateDto.getPrice() != null) {
            product.setPrice(updateDto.getPrice());
        }
        if (updateDto.getImageUrl() != null) {
            product.setImageUrl(updateDto.getImageUrl());
        }
        if (updateDto.getStockQuantity() != null) {
            product.setStockQuantity(updateDto.getStockQuantity());
        }
        if (updateDto.getRating() != null) {
            product.setRating(updateDto.getRating());
        }
        if (updateDto.getOldPrice() != null) {
            product.setOldPrice(updateDto.getOldPrice());
        }
        if (updateDto.getDiscountPercentage() != null) {
            product.setDiscountPercentage(updateDto.getDiscountPercentage());
        }
        if (updateDto.getSizeOptions() != null) {
            product.setSizeOptions(updateDto.getSizeOptions());
        }
        if (updateDto.getCategory() != null) {
            product.setCategory(updateDto.getCategory());
        }

        Product updatedProduct = productRepository.save(product);
        return convertToDto(updatedProduct);
    }

    /**
     * Saves a Product entity directly. Used internally, e.g., by CartService for stock updates.
     * @param product The Product entity to save.
     * @return The saved Product entity.
     */
    @Transactional
    public Product saveProductEntity(Product product) {
        return productRepository.save(product);
    }

    /**
     * Deletes a product by its ID.
     * @param id The ID of the product to delete.
     * @throws RuntimeException if product not found.
     */
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Converts a Product entity to a ProductDto.
     * @param product The Product entity to convert.
     * @return The corresponding ProductDto.
     */
    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setPurchasePrice(product.getPurchasePrice());
        dto.setImageUrl(product.getImageUrl());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setRating(product.getRating());
        dto.setOldPrice(product.getOldPrice());
        dto.setDiscountPercentage(product.getDiscountPercentage());
        dto.setSizeOptions(product.getSizeOptions());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        dto.setCategory(product.getCategory());
        return dto;
    }

    /**
     * Searches for products by a given search term in their name or description (case-insensitive).
     * @param searchTerm The term to search for.
     * @return A list of matching ProductDto.
     */
    public List<ProductDto> searchProducts(String searchTerm) {
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchTerm, searchTerm).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // In ProductService.java

    public Page<ProductDto> searchProducts(String searchTerm, Pageable pageable) {
        return productRepository.searchByNameOrDescription(searchTerm, pageable)
                .map(this::convertToDto);
    }
}