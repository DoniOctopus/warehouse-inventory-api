package com.ifg.warehouse.order;

import com.ifg.warehouse.catalog.dto.CreateItemRequest;
import com.ifg.warehouse.catalog.dto.CreateVariantRequest;
import com.ifg.warehouse.catalog.dto.ItemResponse;
import com.ifg.warehouse.catalog.repository.VariantRepository;
import com.ifg.warehouse.catalog.service.ItemService;
import com.ifg.warehouse.common.exception.InsufficientStockException;
import com.ifg.warehouse.inventory.repository.StockMovementRepository;
import com.ifg.warehouse.order.dto.PlaceOrderRequest;
import com.ifg.warehouse.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies transactional guarantees of order placement.
 * <p>
 * Not @Transactional on purpose: each order must run in its own real transaction that
 * really commits or rolls back, exactly like separate HTTP requests would.
 * Each test uses its own SKUs so leftover data cannot interfere.
 */
@SpringBootTest
class OrderTransactionTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Test
    void failedOrderRollsBackStockOfAllLines() {
        ItemResponse item = itemService.create(new CreateItemRequest(
                "Rollback Check Sneakers", null, new BigDecimal("300000"),
                List.of(new CreateVariantRequest("SNK-42", "EU 42", null, 10, null),
                        new CreateVariantRequest("SNK-43", "EU 43", null, 1, null))));
        long size42 = item.variants().get(0).id();
        long size43 = item.variants().get(1).id();

        // first line can be fulfilled, second cannot
        assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(List.of(
                new PlaceOrderRequest.Line(size42, 5),
                new PlaceOrderRequest.Line(size43, 2)), null)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stockOf(size42)).isEqualTo(10);
        assertThat(stockOf(size43)).isEqualTo(1);
        // only the INITIAL movement exists, no SALE was persisted
        assertThat(movementRepository.findByVariantId(size42, Pageable.unpaged()).getTotalElements()).isEqualTo(1);
    }

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        int initialStock = 5;
        int buyers = 20;
        ItemResponse item = itemService.create(new CreateItemRequest(
                "Limited Edition Poster", null, new BigDecimal("50000"),
                List.of(new CreateVariantRequest("POSTER-LTD", "A2", null, initialStock, null))));
        long variantId = item.variants().get(0).id();

        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < buyers; i++) {
                Callable<Boolean> buyOne = () -> {
                    startSignal.await();
                    try {
                        orderService.placeOrder(new PlaceOrderRequest(
                                List.of(new PlaceOrderRequest.Line(variantId, 1)), null));
                        return true;
                    } catch (InsufficientStockException e) {
                        return false;
                    }
                };
                results.add(pool.submit(buyOne));
            }
            startSignal.countDown(); // release all buyers at once

            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            assertThat(succeeded).isEqualTo(initialStock);
            assertThat(stockOf(variantId)).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    private int stockOf(long variantId) {
        return variantRepository.findById(variantId).orElseThrow().getStockQuantity();
    }
}
