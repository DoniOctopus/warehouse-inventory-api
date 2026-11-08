package com.ifg.warehouse.order.service;

import com.ifg.warehouse.catalog.domain.Variant;
import com.ifg.warehouse.catalog.repository.VariantRepository;
import com.ifg.warehouse.common.exception.BusinessRuleException;
import com.ifg.warehouse.common.exception.ResourceNotFoundException;
import com.ifg.warehouse.common.web.PageResponse;
import com.ifg.warehouse.inventory.domain.MovementType;
import com.ifg.warehouse.inventory.service.StockLedger;
import com.ifg.warehouse.order.domain.Order;
import com.ifg.warehouse.order.domain.OrderLine;
import com.ifg.warehouse.order.domain.OrderStatus;
import com.ifg.warehouse.order.dto.OrderResponse;
import com.ifg.warehouse.order.dto.PlaceOrderRequest;
import com.ifg.warehouse.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final VariantRepository variantRepository;
    private final StockLedger stockLedger;
    private final OrderNumberGenerator orderNumberGenerator;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository,
                        VariantRepository variantRepository,
                        StockLedger stockLedger,
                        OrderNumberGenerator orderNumberGenerator,
                        Clock clock) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.stockLedger = stockLedger;
        this.orderNumberGenerator = orderNumberGenerator;
        this.clock = clock;
    }

    /**
     * Places an order and deducts stock for every line, all or nothing.
     * <p>
     * How overselling is prevented:
     * <ol>
     *   <li>Each variant row is locked with {@code SELECT ... FOR UPDATE} before its stock is read,
     *       so concurrent orders for the same variant are serialised.</li>
     *   <li>Variants are locked in ascending id order, so two orders containing the same
     *       variants can never deadlock each other.</li>
     *   <li>If any line lacks stock, an {@code InsufficientStockException} is thrown and the whole
     *       transaction rolls back: no partial orders, no stock lost.</li>
     *   <li>A {@code CHECK (stock_quantity >= 0)} constraint backs this up at the database level.</li>
     * </ol>
     */
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Order order = new Order(orderNumberGenerator.next(), request.note());

        for (Map.Entry<Long, Integer> line : quantitiesByVariantId(request).entrySet()) {
            Long variantId = line.getKey();
            int quantity = line.getValue();

            Variant variant = variantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "Variant %d does not exist".formatted(variantId)));

            order.addLine(variant, quantity);
            stockLedger.apply(variant, -quantity, MovementType.SALE, null, order.getOrderNumber());
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    /**
     * Cancels a completed order and returns its units to stock.
     * Lines whose variant has since been deleted are skipped.
     */
    public OrderResponse cancel(Long orderId) {
        Order order = findOrder(orderId);
        order.cancel(clock.instant());

        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderLine line : order.getLines()) {
            quantities.merge(line.getVariantId(), line.getQuantity(), Integer::sum);
        }
        quantities.forEach((variantId, quantity) ->
                variantRepository.findByIdForUpdate(variantId).ifPresent(variant ->
                        stockLedger.apply(variant, quantity, MovementType.SALE_CANCELLED,
                                "Order cancelled", order.getOrderNumber())));

        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId) {
        return OrderResponse.from(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(OrderStatus status, Pageable pageable) {
        Page<Order> page = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return PageResponse.of(page, OrderResponse::from);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    /**
     * Merges repeated variants into one line and sorts by variant id (the lock order).
     */
    private static Map<Long, Integer> quantitiesByVariantId(PlaceOrderRequest request) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (PlaceOrderRequest.Line line : request.items()) {
            quantities.merge(line.variantId(), line.quantity(), Math::addExact);
        }
        return quantities;
    }
}
