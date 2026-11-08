package com.ifg.warehouse.catalog.web;

import com.ifg.warehouse.catalog.dto.CreateVariantRequest;
import com.ifg.warehouse.catalog.dto.UpdateVariantRequest;
import com.ifg.warehouse.catalog.dto.VariantResponse;
import com.ifg.warehouse.catalog.service.VariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/items/{itemId}/variants")
@Tag(name = "Variants", description = "Sellable versions (SKUs) of an item, e.g. size or colour")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping
    @Operation(summary = "Add a variant to an item")
    public ResponseEntity<VariantResponse> create(@PathVariable Long itemId,
                                                  @Valid @RequestBody CreateVariantRequest request) {
        VariantResponse created = variantService.create(itemId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List the variants of an item")
    public List<VariantResponse> list(@PathVariable Long itemId) {
        return variantService.list(itemId);
    }

    @GetMapping("/{variantId}")
    @Operation(summary = "Get a single variant")
    public VariantResponse get(@PathVariable Long itemId, @PathVariable Long variantId) {
        return variantService.get(itemId, variantId);
    }

    @PutMapping("/{variantId}")
    @Operation(summary = "Update a variant's SKU, name, price and attributes (not stock)")
    public VariantResponse update(@PathVariable Long itemId, @PathVariable Long variantId,
                                  @Valid @RequestBody UpdateVariantRequest request) {
        return variantService.update(itemId, variantId, request);
    }

    @DeleteMapping("/{variantId}")
    @Operation(summary = "Delete a variant")
    public ResponseEntity<Void> delete(@PathVariable Long itemId, @PathVariable Long variantId) {
        variantService.delete(itemId, variantId);
        return ResponseEntity.noContent().build();
    }
}
