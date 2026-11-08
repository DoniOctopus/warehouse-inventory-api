package com.ifg.warehouse.order.domain;

import com.ifg.warehouse.catalog.domain.Variant;
import com.ifg.warehouse.common.exception.ConflictException;
import com.ifg.warehouse.common.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sale of one or more variants. Creating an order deducts stock immediately
 * (there is no separate payment or fulfilment step in this system).
 */
@Entity
@Table(name = "orders")
public class Order extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 32, updatable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Prevents two concurrent cancellations from both returning stock. */
    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
        // for JPA
    }

    public Order(String orderNumber, String note) {
        this.orderNumber = orderNumber;
        this.note = note;
        this.status = OrderStatus.COMPLETED;
    }

    /** Adds a line priced at the variant's current effective price. */
    public void addLine(Variant variant, int quantity) {
        OrderLine line = new OrderLine(this, variant, quantity);
        lines.add(line);
        totalAmount = totalAmount.add(line.getLineTotal());
    }

    public void cancel(Instant now) {
        if (status == OrderStatus.CANCELLED) {
            throw new ConflictException("Order %s is already cancelled".formatted(orderNumber));
        }
        status = OrderStatus.CANCELLED;
        cancelledAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getNote() {
        return note;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
