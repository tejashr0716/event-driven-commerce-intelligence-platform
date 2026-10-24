package com.infosys.commerce.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    List<OrderItemDto> items,

    @NotNull(message = "Shipping address is mandatory")
    String shippingAddress,

    String currency
) {}
