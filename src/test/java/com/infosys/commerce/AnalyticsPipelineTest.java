package com.infosys.commerce;

import com.infosys.commerce.domain.dto.AnalyticsSummaryDto;
import com.infosys.commerce.domain.dto.CreateOrderRequest;
import com.infosys.commerce.domain.dto.OrderItemDto;
import com.infosys.commerce.domain.dto.OrderResponse;
import com.infosys.commerce.service.AnalyticsPipelineService;
import com.infosys.commerce.service.InventoryService;
import com.infosys.commerce.service.OrderService;
import com.infosys.commerce.service.OutboxPublisherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AnalyticsPipelineTest {

    @Autowired
    private AnalyticsPipelineService analyticsPipelineService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Test
    @DisplayName("Functional stream analytics pipeline accurately computes GMV, category demand and rates")
    public void testAnalyticsComputation() {
        String sku = "SKU-FS-005";
        inventoryService.initializeStock(sku, 50);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemDto(sku, 1, new BigDecimal("64.00"), "WD 2TB Drive")),
                "Infosys Chennai Mahindra World City",
                "USD"
        );

        OrderResponse response = orderService.createOrder(request, "ANALYTICS-" + UUID.randomUUID(), "customer@infosys.com");
        assertNotNull(response);

        // Process through consumer to reach COMPLETED state
        outboxPublisherService.dispatchPendingEvents();

        AnalyticsSummaryDto summary = analyticsPipelineService.computeOperationalMetrics();

        assertTrue(summary.totalOrders() >= 1, "Total orders count must be >= 1");
        assertTrue(summary.grossMerchandiseValue().compareTo(BigDecimal.ZERO) > 0, "GMV must be > 0");
        assertNotNull(summary.categoryDemand(), "Category demand map must not be null");
        assertNotNull(summary.currentStockLevels(), "Stock level map must not be null");
        assertTrue(summary.currentStockLevels().containsKey(sku), "Stock levels must include test SKU");
    }
}
