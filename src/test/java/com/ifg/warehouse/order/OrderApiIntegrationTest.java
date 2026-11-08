package com.ifg.warehouse.order;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long itemId;
    private long blackM;   // 10 in stock, inherits base price 99,000
    private long blackXl;  // 2 in stock, own price 109,000

    @BeforeEach
    void createCatalog() throws Exception {
        JsonNode item = json(mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Hoodie", "basePrice": 99000,
                                  "variants": [
                                    { "sku": "HD-BLK-M",  "name": "Black / M",  "initialStock": 10 },
                                    { "sku": "HD-BLK-XL", "name": "Black / XL", "initialStock": 2, "price": 109000 }
                                  ] }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        itemId = item.get("id").asLong();
        blackM = item.at("/variants/0/id").asLong();
        blackXl = item.at("/variants/1/id").asLong();
    }

    @Test
    void placingAnOrderDeductsStockAndSnapshotsPrices() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(blackM, 3, blackXl, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(matchesPattern("ORD-\\d{8}-[A-Z0-9]{6}")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(99000))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(297000))
                .andExpect(jsonPath("$.lines[1].unitPrice").value(109000))
                .andExpect(jsonPath("$.totalAmount").value(515000));

        assertStock(blackM, 7);
        assertStock(blackXl, 0);

        mvc.perform(get("/api/items/{id}", itemId))
                .andExpect(jsonPath("$.variants[1].inStock").value(false));
    }

    @Test
    void cannotSellMoreThanIsInStock() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(blackXl, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.sku").value("HD-BLK-XL"))
                .andExpect(jsonPath("$.requested").value(3))
                .andExpect(jsonPath("$.available").value(2));

        assertStock(blackXl, 2);
    }

    @Test
    void repeatedVariantLinesAreMergedBeforeCheckingStock() throws Exception {
        // 2 + 1 = 3 units of a variant that only has 2
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(blackXl, 2, blackXl, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.requested").value(3));
    }

    @Test
    void cancellingAnOrderReturnsStock() throws Exception {
        long orderId = json(mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(blackM, 4)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        assertStock(blackM, 6);

        mvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
        assertStock(blackM, 10);

        mvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict());
        assertStock(blackM, 10);

        mvc.perform(get("/api/variants/{id}/stock-movements", blackM))
                .andExpect(jsonPath("$.content[0].type").value("SALE_CANCELLED"))
                .andExpect(jsonPath("$.content[1].type").value("SALE"))
                .andExpect(jsonPath("$.content[1].quantityChange").value(-4));
    }

    @Test
    void orderKeepsOriginalPriceAfterRepricing() throws Exception {
        long orderId = json(mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(blackM, 1)))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/items/{id}", itemId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Hoodie", "basePrice": 150000 }
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(99000));
    }

    @Test
    void listsAndFiltersOrders() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(blackM, 1)));
        long toCancel = json(mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(blackM, 1)))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/orders/{id}/cancel", toCancel));

        mvc.perform(get("/api/orders"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/orders").param("status", "CANCELLED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(toCancel));
        mvc.perform(get("/api/orders").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidOrders() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [] }
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(blackM, 0)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(987_654L, 1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Variant 987654 does not exist"));
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mvc.perform(get("/api/orders/{id}", 123_456))
                .andExpect(status().isNotFound());
    }

    private void assertStock(long variantId, int expected) throws Exception {
        mvc.perform(get("/api/items/{itemId}/variants/{variantId}", itemId, variantId))
                .andExpect(jsonPath("$.stockQuantity").value(expected));
    }

    private static String order(long variantId, int quantity) {
        return """
                { "items": [ { "variantId": %d, "quantity": %d } ] }
                """.formatted(variantId, quantity);
    }

    private static String order(long v1, int q1, long v2, int q2) {
        return """
                { "items": [ { "variantId": %d, "quantity": %d }, { "variantId": %d, "quantity": %d } ] }
                """.formatted(v1, q1, v2, q2);
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}
