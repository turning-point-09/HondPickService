package com.example.handPick.dao; // Adjusted package name

import com.example.handPick.entity.Product; // Adjusted import for Product entity
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param; // Correct import for @Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
// Removed: import org.springframework.web.bind.annotation.RequestParam; // This is not for repositories

@RepositoryRestResource // Exposes this repository as a REST endpoint (default path and rel will be used)
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Custom query method: Spring Data JPA will generate the query to find
    // products by category ID, with pagination support.
    // @Param("id") maps the method parameter to the named parameter 'id' for the REST endpoint.
    Page<Product> findByCategoryId(@Param("id") Long id, Pageable pageable);

    // Custom query method: Spring Data JPA will generate the query to find
    // products whose name contains the given string, with pagination support.
    // @Param("name") maps the method parameter to the named parameter 'name' for the REST endpoint.
    Page<Product> findByNameContaining(@Param("name") String name, Pageable pageable);

}