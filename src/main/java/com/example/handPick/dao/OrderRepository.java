package com.example.handPick.dao; // Adjusted package name

import com.example.handPick.entity.Order; // Adjusted import for Order entity
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource // Exposes this repository as a REST endpoint (default path and rel will be used)
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Custom query method: Spring Data JPA will generate the query to find
    // orders by customer email, with pagination support.
    // @Param("email") maps the method parameter to the named parameter in the query (if one were explicit)
    // or to the URL parameter when exposed via Spring Data REST.
    Page<Order> findByCustomerEmail(@Param("email") String email, Pageable pageable);
}