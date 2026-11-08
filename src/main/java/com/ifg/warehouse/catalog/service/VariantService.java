package com.ifg.warehouse.catalog.service;

import com.ifg.warehouse.catalog.domain.Item;
import com.ifg.warehouse.catalog.domain.Variant;
import com.ifg.warehouse.catalog.dto.CreateVariantRequest;
import com.ifg.warehouse.catalog.dto.UpdateVariantRequest;
import com.ifg.warehouse.catalog.dto.VariantResponse;
import com.ifg.warehouse.catalog.repository.VariantRepository;
import com.ifg.warehouse.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VariantService {

    private final ItemService itemService;
    private final VariantRepository variantRepository;
    private final SkuPolicy skuPolicy;

    public VariantService(ItemService itemService, VariantRepository variantRepository, SkuPolicy skuPolicy) {
        this.itemService = itemService;
        this.variantRepository = variantRepository;
        this.skuPolicy = skuPolicy;
    }

    public VariantResponse create(Long itemId, CreateVariantRequest request) {
        Item item = itemService.findItem(itemId);
        skuPolicy.assertAvailable(List.of(SkuPolicy.normalize(request.sku())));

        Variant variant = ItemService.toVariant(request);
        item.addVariant(variant);
        // Flush so the generated id and timestamps are available for the response.
        return VariantResponse.from(variantRepository.saveAndFlush(variant));
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> list(Long itemId) {
        return itemService.findItem(itemId).getVariants().stream()
                .map(VariantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse get(Long itemId, Long variantId) {
        return VariantResponse.from(findVariant(itemId, variantId));
    }

    public VariantResponse update(Long itemId, Long variantId, UpdateVariantRequest request) {
        Variant variant = findVariant(itemId, variantId);
        String sku = SkuPolicy.normalize(request.sku());
        skuPolicy.assertAvailableFor(sku, variantId);

        variant.updateDetails(sku, request.name().trim(), request.price(), request.attributes());
        return VariantResponse.from(variantRepository.saveAndFlush(variant));
    }

    public void delete(Long itemId, Long variantId) {
        Variant variant = findVariant(itemId, variantId);
        variant.getItem().removeVariant(variant);
    }

    private Variant findVariant(Long itemId, Long variantId) {
        return variantRepository.findByIdAndItemId(variantId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Variant %d was not found for item %d".formatted(variantId, itemId)));
    }
}
