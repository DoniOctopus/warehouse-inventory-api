package com.ifg.warehouse.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long itemId;
    private long variantId;

    @BeforeEach
    void createVariantWithStock() throws Exception {
        String body = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Coffee Mug", "basePrice": 55000,
                                  "variants": [ { "sku": "MUG-WHT", "name": "White", "initialStock": 4 } ] }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var json = objectMapper.readTree(body);
        itemId = json.get("id").asLong();
        variantId = json.at("/variants/0/id").asLong();
    }

    @Test
    void restockIncreasesStockAndIsRecordedInHistory() throws Exception {
        mvc.perform(post("/api/variants/{id}/stock-movements", variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "RESTOCK", "quantity": 10, "reason": "Supplier delivery #42" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RESTOCK"))
                .andExpect(jsonPath("$.quantityChange").value(10))
                .andExpect(jsonPath("$.quantityAfter").value(14));

        mvc.perform(get("/api/items/{itemId}/variants/{variantId}", itemId, variantId))
                .andExpect(jsonPath("$.stockQuantity").value(14));

        mvc.perform(get("/api/variants/{id}/stock-movements", variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                // newest first
                .andExpect(jsonPath("$.content[0].type").value("RESTOCK"))
                .andExpect(jsonPath("$.content[1].type").value("INITIAL"))
                .andExpect(jsonPath("$.content[1].quantityAfter").value(4));
    }

    @Test
    void negativeAdjustmentRemovesStock() throws Exception {
        mvc.perform(post("/api/variants/{id}/stock-movements", variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "ADJUSTMENT", "quantity": -3, "reason": "Broken during handling" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityAfter").value(1));
    }

    @Test
    void adjustmentCannotTakeStockBelowZero() throws Exception {
        mvc.perform(post("/api/variants/{id}/stock-movements", variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "ADJUSTMENT", "quantity": -5 }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.available").value(4))
                .andExpect(jsonPath("$.requested").value(5));
    }

    @Test
    void restockMustBePositive() throws Exception {
        mvc.perform(post("/api/variants/{id}/stock-movements", variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "RESTOCK", "quantity": -1 }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void salesCannotBeRecordedManually() throws Exception {
        mvc.perform(post("/api/variants/{id}/stock-movements", variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "SALE", "quantity": -1 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownVariant() throws Exception {
        mvc.perform(post("/api/variants/{id}/stock-movements", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "RESTOCK", "quantity": 1 }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsLowStockVariants() throws Exception {
        mvc.perform(get("/api/inventory/low-stock").param("threshold", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'MUG-WHT')].stockQuantity").value(4));

        mvc.perform(get("/api/inventory/low-stock").param("threshold", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'MUG-WHT')]").isEmpty());

        mvc.perform(get("/api/inventory/low-stock").param("threshold", "-1"))
                .andExpect(status().isBadRequest());
    }
}
