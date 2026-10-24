package com.infosys.commerce.domain.dto;

import com.infosys.commerce.domain.enums.DataSourceMode;
import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String sku,
    String title,
    String description,
    String category,
    BigDecimal price,
    String currency,
    String imageUrl,
    DataSourceMode sourceOrigin,
    int availableStock
) {}
