package com.ifg.warehouse.order.dto;

import com.ifg.warehouse.order.domain.Order;
import com.ifg.warehouse.order.domain.OrderLine;
import com.ifg.warehouse.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        String note,
        List<Line> lines,
        Instant createdAt,
        Instant cancelledAt
) {

    public record Line(
            Long variantId,
            String sku,
            String itemName,
            String variantName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {

        static Line from(OrderLine line) {
            return new Line(
                    line.getVariantId(),
                    line.getSku(),
                    line.getItemName(),
                    line.getVariantName(),
                    line.getUnitPrice(),
                    line.getQuantity(),
                    line.getLineTotal()
            );
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getNote(),
                order.getLines().stream().map(Line::from).toList(),
                order.getCreatedAt(),
                order.getCancelledAt()
        );
    }
}
