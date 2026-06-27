package com.picnic.psi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a product that can appear inside a section.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /** Auto-generated product ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Product display name, e.g. "Organic Bananas" */
    private String name;

    /** Price in euros */
    private Double price;

    /** Product category, e.g. "fruits", "bakery", "dairy" */
    private String category;

    /** URL to the product image */
    private String imageUrl;

    /** The section this product belongs to (references Section.id) */
    private String sectionId;
}
