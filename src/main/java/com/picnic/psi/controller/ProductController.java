package com.picnic.psi.controller;

import com.picnic.psi.model.Product;
import com.picnic.psi.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog endpoints")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Returns all products grouped by category
    // Used by the customer panel to show buyable products
    @GetMapping
    @Operation(summary = "Get all products grouped by category")
    public ResponseEntity<Map<String, List<Product>>> getAllProductsGrouped() {
        List<Product> all = productRepository.findAll();
        Map<String, List<Product>> grouped = all.stream()
                .collect(Collectors.groupingBy(Product::getCategory));
        return ResponseEntity.ok(grouped);
    }
}
