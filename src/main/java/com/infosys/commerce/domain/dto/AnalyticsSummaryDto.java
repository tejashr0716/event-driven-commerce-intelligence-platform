package com.infosys.commerce.domain.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsSummaryDto(
    long totalOrders,
    long completedOrders,
    long cancelledOrders,
    long failedOrders,
    BigDecimal grossMerchandiseValue,
    double reservationFailureRatePercent,
    double cancellationRatePercent,
    long pendingOutboxEvents,
    Map<String, Long> categoryDemand,
    Map<String, Integer> currentStockLevels
) {}
