package com.picnic.psi.repository;

import com.picnic.psi.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Page entities.
 * Built-in findById and findAll from JpaRepository are sufficient.
 */
@Repository
public interface PageRepository extends JpaRepository<Page, String> {
}
