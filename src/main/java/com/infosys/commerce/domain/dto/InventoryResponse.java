package com.infosys.commerce.domain.dto;

import java.time.Instant;

public record InventoryResponse(
    String sku,
    int totalStock,
    int reservedStock,
    int availableStock,
    Instant updatedAt
) {
    public InventoryResponse(String sku, int totalStock, int reservedStock, int availableStock) {
        this(sku, totalStock, reservedStock, availableStock, Instant.now());
    }
}
