package com.ifg.warehouse.catalog.web;

import com.ifg.warehouse.catalog.dto.CreateItemRequest;
import com.ifg.warehouse.catalog.dto.ItemResponse;
import com.ifg.warehouse.catalog.dto.UpdateItemRequest;
import com.ifg.warehouse.catalog.service.ItemService;
import com.ifg.warehouse.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Items", description = "Products sold by the shop")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @Operation(summary = "Create an item, optionally with its variants")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        ItemResponse created = itemService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List items, optionally filtered by name (paginated)")
    public PageResponse<ItemResponse> list(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return itemService.search(name, pageable);
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Get an item with its variants")
    public ItemResponse get(@PathVariable Long itemId) {
        return itemService.get(itemId);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update an item's name, description and base price")
    public ItemResponse update(@PathVariable Long itemId, @Valid @RequestBody UpdateItemRequest request) {
        return itemService.update(itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete an item and all of its variants")
    public ResponseEntity<Void> delete(@PathVariable Long itemId) {
        itemService.delete(itemId);
        return ResponseEntity.noContent().build();
    }
}
