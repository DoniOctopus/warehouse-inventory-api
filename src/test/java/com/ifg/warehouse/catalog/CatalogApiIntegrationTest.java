package com.ifg.warehouse.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TSHIRT = """
            {
              "name": "Basic T-Shirt",
              "description": "100% cotton",
              "basePrice": 99000,
              "variants": [
                { "sku": "ts-blk-m", "name": "Black / M", "initialStock": 10,
                  "attributes": { "color": "Black", "size": "M" } },
                { "sku": "TS-BLK-XL", "name": "Black / XL", "price": 109000, "initialStock": 3,
                  "attributes": { "color": "Black", "size": "XL" } }
              ]
            }
            """;

    @Test
    void createsItemWithVariantsAndComputesPricesAndStock() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(TSHIRT))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/items/")))
                .andExpect(jsonPath("$.name").value("Basic T-Shirt"))
                .andExpect(jsonPath("$.totalStock").value(13))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.variants", hasSize(2)))
                // SKUs are normalised to upper case
                .andExpect(jsonPath("$.variants[0].sku").value("TS-BLK-M"))
                // variant without its own price inherits the item's base price
                .andExpect(jsonPath("$.variants[0].price").doesNotExist())
                .andExpect(jsonPath("$.variants[0].effectivePrice").value(99000))
                .andExpect(jsonPath("$.variants[1].effectivePrice").value(109000))
                .andExpect(jsonPath("$.variants[1].attributes.size").value("XL"));
    }

    @Test
    void getsListsUpdatesAndDeletesItem() throws Exception {
        long itemId = createItem(TSHIRT);

        mvc.perform(get("/api/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId));

        mvc.perform(get("/api/items").param("name", "t-shirt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(put("/api/items/{id}", itemId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Premium T-Shirt", "description": "Organic cotton", "basePrice": 120000 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium T-Shirt"))
                // inherited prices follow the new base price
                .andExpect(jsonPath("$.variants[0].effectivePrice").value(120000));

        mvc.perform(delete("/api/items/{id}", itemId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/items/{id}", itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void managesVariantsOfAnItem() throws Exception {
        long itemId = createItem("""
                { "name": "Canvas Tote Bag", "basePrice": 75000 }
                """);

        String created = mvc.perform(post("/api/items/{id}/variants", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sku": "TOTE-NAT", "name": "Natural", "initialStock": 5,
                                  "attributes": { "color": "Natural" } }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").value(itemId))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andReturn().getResponse().getContentAsString();
        long variantId = id(created);

        mvc.perform(put("/api/items/{itemId}/variants/{variantId}", itemId, variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sku": "TOTE-NAT", "name": "Natural Beige", "price": 80000,
                                  "attributes": { "color": "Beige" } }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Natural Beige"))
                .andExpect(jsonPath("$.effectivePrice").value(80000))
                .andExpect(jsonPath("$.attributes.color").value("Beige"))
                // stock is not editable through the variant endpoint
                .andExpect(jsonPath("$.stockQuantity").value(5));

        mvc.perform(get("/api/items/{itemId}/variants", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/items/{itemId}/variants/{variantId}", itemId, variantId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/items/{itemId}/variants/{variantId}", itemId, variantId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateSku() throws Exception {
        createItem(TSHIRT);

        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Another shirt", "basePrice": 50000,
                                  "variants": [ { "sku": "TS-BLK-M", "name": "Clash" } ] }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("SKU 'TS-BLK-M' is already in use"));
    }

    @Test
    void rejectsInvalidPayloadWithFieldErrors() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "", "basePrice": -1,
                                  "variants": [ { "sku": "bad sku!", "name": "x", "initialStock": -5 } ] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(4)));
    }

    @Test
    void returnsNotFoundForVariantOfAnotherItem() throws Exception {
        long first = createItem(TSHIRT);
        long second = createItem("""
                { "name": "Cap", "basePrice": 45000 }
                """);
        long variantOfFirst = objectMapper.readTree(
                        mvc.perform(get("/api/items/{id}", first)).andReturn().getResponse().getContentAsString())
                .at("/variants/0/id").asLong();

        mvc.perform(get("/api/items/{itemId}/variants/{variantId}", second, variantOfFirst))
                .andExpect(status().isNotFound());
    }

    private long createItem(String json) throws Exception {
        String body = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    private long id(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}
