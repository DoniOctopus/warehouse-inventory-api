package com.ifg.warehouse.catalog.service;

import com.ifg.warehouse.catalog.domain.Item;
import com.ifg.warehouse.catalog.domain.Variant;
import com.ifg.warehouse.catalog.dto.CreateItemRequest;
import com.ifg.warehouse.catalog.dto.CreateVariantRequest;
import com.ifg.warehouse.catalog.dto.ItemResponse;
import com.ifg.warehouse.catalog.dto.UpdateItemRequest;
import com.ifg.warehouse.catalog.repository.ItemRepository;
import com.ifg.warehouse.common.exception.ResourceNotFoundException;
import com.ifg.warehouse.common.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final SkuPolicy skuPolicy;

    public ItemService(ItemRepository itemRepository, SkuPolicy skuPolicy) {
        this.itemRepository = itemRepository;
        this.skuPolicy = skuPolicy;
    }

    public ItemResponse create(CreateItemRequest request) {
        skuPolicy.assertAvailable(request.variantsOrEmpty().stream()
                .map(v -> SkuPolicy.normalize(v.sku()))
                .toList());

        Item item = new Item(request.name().trim(), request.description(), request.basePrice());
        request.variantsOrEmpty().forEach(v -> item.addVariant(toVariant(v)));

        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        return ItemResponse.from(findItem(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> search(String name, Pageable pageable) {
        Page<Item> page = StringUtils.hasText(name)
                ? itemRepository.findByNameContainingIgnoreCase(name.trim(), pageable)
                : itemRepository.findAll(pageable);
        return PageResponse.of(page, ItemResponse::from);
    }

    public ItemResponse update(Long id, UpdateItemRequest request) {
        Item item = findItem(id);
        item.updateDetails(request.name().trim(), request.description(), request.basePrice());
        // Flush so the response carries the refreshed updatedAt timestamp.
        return ItemResponse.from(itemRepository.saveAndFlush(item));
    }

    public void delete(Long id) {
        itemRepository.delete(findItem(id));
    }

    Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", id));
    }

    static Variant toVariant(CreateVariantRequest request) {
        return new Variant(
                SkuPolicy.normalize(request.sku()),
                request.name().trim(),
                request.price(),
                request.initialStockOrZero(),
                request.attributes());
    }
}
