package com.picnic.psi.repository;

import java.util.List;

import com.picnic.psi.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Product entities.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Fetches all products that belong to a given section, used to populate product grids
    List<Product> findBySectionId(String sectionId);
}
