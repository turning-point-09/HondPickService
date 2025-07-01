package com.example.handPick.service;

import com.example.handPick.dto.ProductDto;
import com.example.handPick.model.Product;
import com.example.handPick.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Retrieve all products as DTOs.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> findAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a single product DTO by its ID.
     * Throws RuntimeException if not found.
     */
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        return findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    /**
     * Expose the raw Product entity wrapped in Optional.
     * Used by CartService to fetch and mutate stock etc.
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductEntityById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Expose an Optional DTO if needed elsewhere.
     */
    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductDtoById(Long id) {
        return findById(id).map(this::convertToDto);
    }

    private Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Save or update a Product via DTO → Entity → DTO.
     */
    @Transactional
    public ProductDto saveProduct(ProductDto productDto) {
        Product product = productDto.getId() != null
                ? productRepository.findById(productDto.getId())
                .orElseThrow(() ->
                        new RuntimeException("Product not found for update: " + productDto.getId()))
                : new Product();

        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(BigDecimal.valueOf(productDto.getPrice()));
        product.setImageUrl(productDto.getImageUrl());
        product.setStockQuantity(productDto.getStockQuantity());
        product.setRating(productDto.getRating());
        product.setOldPrice(
                productDto.getOldPrice() != null
                        ? BigDecimal.valueOf(productDto.getOldPrice())
                        : null
        );
        product.setDiscountPercentage(productDto.getDiscountPercentage());

        Product saved = productRepository.save(product);
        return convertToDto(saved);
    }

    /**
     * Directly save or update a Product entity.
     * Used internally by CartService when adjusting stock.
     */
    @Transactional
    public Product saveProductEntity(Product product) {
        return productRepository.save(product);
    }

    /**
     * Delete a product by ID.
     */
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // ----------------------------------------------------------------
    // Conversion helper: Entity → DTO
    // ----------------------------------------------------------------
    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice().doubleValue());
        dto.setImageUrl(product.getImageUrl());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setRating(product.getRating());
        dto.setOldPrice(
                product.getOldPrice() != null
                        ? product.getOldPrice().doubleValue()
                        : null
        );
        dto.setDiscountPercentage(product.getDiscountPercentage());
        return dto;
    }
}
