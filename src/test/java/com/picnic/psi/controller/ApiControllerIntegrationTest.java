package com.picnic.psi.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
final class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageRanksSectionsIncludesScoreBreakdownAndServerTiming() throws Exception {
        mockMvc.perform(get("/api/pages/home").param("customerId", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Server-Timing", containsString("page-assembly;dur=")))
                .andExpect(jsonPath("$.customerId", is("customer-1")))
                .andExpect(jsonPath("$.sections[0].sectionId", is("banner")))
                .andExpect(jsonPath("$.sections[1].sectionId", is("dairy")))
                .andExpect(jsonPath("$.sections[1].scoreBreakdown.purchaseCount", is(12)))
                .andExpect(jsonPath("$.sections[1].score", greaterThan(0.7)));
    }

    @Test
    void productDetailPageReturnsServerDrivenSections() throws Exception {
        mockMvc.perform(get("/api/pages/product-detail").param("customerId", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId", is("product-detail")))
                .andExpect(jsonPath("$.sections.length()", is(2)));
    }

    @Test
    void unknownPageReturnsProblemDetail404() throws Exception {
        mockMvc.perform(get("/api/pages/does-not-exist/scores").param("customerId", "customer-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource not found")));
    }

    @Test
    void invalidPurchaseCategoryReturnsBadRequest400() throws Exception {
        mockMvc.perform(post("/api/customers/alice/purchase").param("category", "unknown-cat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid request")));
    }

    @Test
    void newCustomerUnlocksRecipesAfterThreePurchasesAndResetsCleanly() throws Exception {
        String customer = "test-shopper";

        // Initially recipes is locked (0 purchases)
        mockMvc.perform(get("/api/customers/{id}/summary", customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPurchases", is(0)))
                .andExpect(jsonPath("$.recipesUnlockProgress", is(0)));

        // Buy 3 dairy items
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/customers/{id}/purchase", customer).param("category", "dairy"))
                    .andExpect(status().isOk());
        }

        // Now recipes progress reaches 100%
        mockMvc.perform(get("/api/customers/{id}/summary", customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPurchases", is(3)))
                .andExpect(jsonPath("$.preference", is("Dairy Lover")))
                .andExpect(jsonPath("$.recipesUnlockProgress", is(100)));

        // Clean up
        mockMvc.perform(delete("/api/customers/{id}/history", customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRecords", is(1)));
    }

    @Test
    void actuatorHealthAndProductsEndpointRespondOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dairy").isArray());
    }
}
