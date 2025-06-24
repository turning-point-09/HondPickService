package com.example.handPick.dao; // Adjusted package name

import com.example.handPick.entity.ProductCategory; // Adjusted import for ProductCategory entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

// @RepositoryRestResource customizes the REST endpoint for this repository.
// collectionResourceRel: The name of the collection resource rel in the HAL JSON output.
// path: The path under which the collection resource is exposed.
@RepositoryRestResource(collectionResourceRel = "productCategory", path = "product-category")
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    // This interface automatically provides CRUD operations for the ProductCategory entity.
    // Spring Data REST will expose these operations at /api/product-category by default.
}