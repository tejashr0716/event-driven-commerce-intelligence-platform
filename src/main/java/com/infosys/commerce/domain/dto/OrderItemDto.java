package com.infosys.commerce.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record OrderItemDto(
    @NotBlank String sku,
    @Min(value = 1, message = "Quantity must be at least 1") int quantity,
    BigDecimal unitPrice,
    String productTitle
) {}
