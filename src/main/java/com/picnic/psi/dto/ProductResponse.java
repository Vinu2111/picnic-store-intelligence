package com.picnic.psi.dto;

import com.picnic.psi.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single product in the API response.
 * Maps 1-to-1 from the Product entity, excluding internal fields like sectionId.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    /** Unique product identifier */
    private Long id;

    /** Product display name, e.g. "Whole Milk 1L" */
    private String name;

    /** Price in euros */
    private Double price;

    /** Product category, e.g. "dairy", "vegetables", "recipes" */
    private String category;

    /** URL to the product image */
    private String imageUrl;

    /**
     * Factory method to convert a Product entity into a ProductResponse DTO.
     *
     * @param product the JPA entity to convert
     * @return a new ProductResponse with all fields mapped
     */
    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .build();
    }
}
