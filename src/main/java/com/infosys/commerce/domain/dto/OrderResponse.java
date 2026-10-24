package com.infosys.commerce.domain.dto;

import com.infosys.commerce.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    Long userId,
    String customerEmail,
    String idempotencyKey,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency,
    String shippingAddress,
    String failureReason,
    List<OrderItemDto> items,
    Instant createdAt,
    Instant updatedAt
) {}
