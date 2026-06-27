package com.picnic.psi.controller;

import java.util.List;

import com.picnic.psi.dto.PageResponse;
import com.picnic.psi.dto.SectionResponse;
import com.picnic.psi.service.PageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the server-driven page layout API.
 * Returns fully assembled, personalized page layouts with scored and visibility-checked sections.
 */
@RestController
@RequestMapping("/api/pages")
@Tag(name = "Page Platform", description = "Server-driven page layout API")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Returns the personalized home page layout.
     * Pass customerId to get personalized section ordering based on purchase history.
     * Omit customerId for the default (anonymous) layout.
     */
    @GetMapping("/home")
    @Operation(summary = "Get personalized home page layout")
    public ResponseEntity<PageResponse> getHomePage(
            @RequestParam(required = false) String customerId) {

        PageResponse response = pageService.buildPage("home", customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the product detail page layout.
     * Currently a simpler page, but follows the same personalization pipeline.
     */
    @GetMapping("/product-detail")
    @Operation(summary = "Get product detail page layout")
    public ResponseEntity<PageResponse> getProductDetailPage(
            @RequestParam(required = false) String customerId) {

        PageResponse response = pageService.buildPage("product-detail", customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns raw section scores for a given page and customer.
     * This endpoint exposes raw scores — perfect for explaining the algorithm in interviews.
     */
    @GetMapping("/{pageId}/scores")
    @Operation(summary = "Get raw section scores for a customer — shows the ranking algorithm in action")
    public ResponseEntity<List<SectionResponse>> getSectionScores(
            @PathVariable String pageId,
            @RequestParam String customerId) {

        PageResponse response = pageService.buildPage(pageId, customerId);
        return ResponseEntity.ok(response.getSections());
    }
}
