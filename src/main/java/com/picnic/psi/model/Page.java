package com.picnic.psi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a page in the store (e.g., Home, Product Detail).
 */
@Entity
@Table(name = "pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {

    /** Manually set page identifier, e.g. "home", "product-detail" */
    @Id
    private String id;

    /** Display title of the page */
    private String title;

    /** Short description of what this page shows */
    private String description;
}
