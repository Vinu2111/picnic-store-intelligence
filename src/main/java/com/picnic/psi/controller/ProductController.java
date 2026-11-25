package com.picnic.psi.controller;

import java.util.List;
import java.util.Map;

import com.picnic.psi.dto.ProductResponse;
import com.picnic.psi.service.StoreCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/products")
@RestController
@Tag(name = "Products", description = "Product catalog endpoints")
public class ProductController {

    private final StoreCatalogService catalogService;

    public ProductController(StoreCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Get all products grouped by category")
    public ResponseEntity<Map<String, List<ProductResponse>>> getAllProductsGrouped() {
        return ResponseEntity.ok(catalogService.getAllProductsGrouped());
    }
}
