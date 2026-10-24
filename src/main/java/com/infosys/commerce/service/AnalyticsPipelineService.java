package com.infosys.commerce.service;

import com.infosys.commerce.domain.dto.AnalyticsSummaryDto;
import com.infosys.commerce.domain.enums.OrderStatus;
import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.domain.model.Order;
import com.infosys.commerce.domain.model.OrderItem;
import com.infosys.commerce.domain.repository.InventoryRepository;
import com.infosys.commerce.domain.repository.OrderRepository;
import com.infosys.commerce.domain.repository.OutboxEventRepository;
import com.infosys.commerce.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsPipelineService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProductRepository productRepository;

    public AnalyticsPipelineService(OrderRepository orderRepository,
                                    InventoryRepository inventoryRepository,
                                    OutboxEventRepository outboxEventRepository,
                                    ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto computeOperationalMetrics() {
        List<Order> allOrders = orderRepository.findAll();
        List<Inventory> allInventory = inventoryRepository.findAll();

        long totalOrders = allOrders.size();

        // Functional stream partitions
        Map<OrderStatus, Long> countsByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        long completed = countsByStatus.getOrDefault(OrderStatus.COMPLETED, 0L);
        long cancelled = countsByStatus.getOrDefault(OrderStatus.CANCELLED, 0L);
        long failed = countsByStatus.getOrDefault(OrderStatus.FAILED, 0L);

        // Functional reduction: Gross Merchandise Value (GMV) of completed orders
        BigDecimal gmv = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Rate calculations
        double failureRate = totalOrders == 0 ? 0.0 : ((double) failed / totalOrders) * 100.0;
        double cancellationRate = totalOrders == 0 ? 0.0 : ((double) cancelled / totalOrders) * 100.0;

        long pendingOutbox = outboxEventRepository.countByStatus("PENDING");

        // Category demand aggregation via functional stream grouping
        Map<String, String> skuToCategory = productRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getSku(), p -> p.getCategory(), (c1, c2) -> c1));

        Map<String, Long> categoryDemand = allOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> skuToCategory.getOrDefault(item.getSku(), "uncategorized"),
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        // Inventory stock level snapshots
        Map<String, Integer> currentStock = allInventory.stream()
                .collect(Collectors.toMap(Inventory::getSku, Inventory::getAvailableStock, (a, b) -> a));

        return new AnalyticsSummaryDto(
                totalOrders,
                completed,
                cancelled,
                failed,
                gmv,
                Math.round(failureRate * 100.0) / 100.0,
                Math.round(cancellationRate * 100.0) / 100.0,
                pendingOutbox,
                categoryDemand,
                currentStock
        );
    }
}
