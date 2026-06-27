package com.picnic.psi.repository;

import java.util.List;

import com.picnic.psi.model.PurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing PurchaseHistory entities.
 */
@Repository
public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {

    // Fetches all purchase history records for a customer, used to personalize their experience
    List<PurchaseHistory> findByCustomerId(String customerId);
}
