package com.example.handPick.dao; // Adjusted package name

import com.example.handPick.entity.State; // Adjusted import for State entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource // Exposes this repository as a REST endpoint (default path and rel will be used)
public interface StateRepository extends JpaRepository<State, Integer> {

    // Custom query method: Spring Data JPA will generate the query to find
    // states based on the country code.
    // @Param("code") maps the method parameter to the named parameter 'code' for the REST endpoint.
    List<State> findByCountryCode(@Param("code") String code);

}