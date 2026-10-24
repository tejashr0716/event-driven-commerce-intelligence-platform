package com.infosys.commerce.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.commerce.domain.enums.OrderStatus;
import com.infosys.commerce.domain.model.Order;
import com.infosys.commerce.domain.model.OrderItem;
import com.infosys.commerce.domain.model.OutboxEvent;
import com.infosys.commerce.domain.model.ProcessedEvent;
import com.infosys.commerce.domain.repository.OrderRepository;
import com.infosys.commerce.domain.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class EventConsumerService {

    private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);

    private final ProcessedEventRepository processedEventRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public EventConsumerService(ProcessedEventRepository processedEventRepository,
                                OrderRepository orderRepository,
                                InventoryService inventoryService,
                                AuditService auditService,
                                ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean processEvent(OutboxEvent event) {
        String eventKey = "outbox-" + event.getId();

        // 1. Consumer Idempotency Check
        if (processedEventRepository.existsById(eventKey)) {
            log.info("Consumer Deduplication: Event {} already processed. Skipping re-execution.", eventKey);
            return true;
        }

        try {
            log.info("Consumer processing event ID={}, Type={}, AggregateId={}",
                    event.getId(), event.getEventType(), event.getAggregateId());

            switch (event.getEventType()) {
                case ORDER_CREATED -> handleOrderCreated(event);
                case ORDER_CANCELLED -> handleOrderCancelled(event);
                default -> log.info("No async handling required for event type {}", event.getEventType());
            }

            // Record as processed in consumer deduplication log
            processedEventRepository.save(new ProcessedEvent(eventKey, "FulfillmentConsumer"));
            return true;
        } catch (Exception e) {
            log.error("Error in consumer processing event {}: {}", event.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private void handleOrderCreated(OutboxEvent event) {
        Long orderId = Long.parseLong(event.getAggregateId());
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order #{} not found during async event consumption", orderId);
            return;
        }

        // Poison Pill simulation: fail orders exceeding $10,000 threshold or flagged
        if (order.getTotalAmount().compareTo(new BigDecimal("10000.00")) > 0) {
            log.warn("Payment Authorization failed for Order #{}: exceeds risk limit threshold ($10,000)", orderId);
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseStock(item.getSku(), item.getQuantity());
            }
            order.transitionTo(OrderStatus.FAILED, "Payment declined: risk limit threshold exceeded");
            orderRepository.save(order);
            auditService.record("PAYMENT_DECLINED", "evt-" + event.getId(), order.getCustomerEmail(),
                    "Order #" + orderId + " failed payment check and released inventory", null);
            return;
        }

        // Transition: INVENTORY_RESERVED -> PAYMENT_AUTHORIZED -> COMPLETED
        if (order.getStatus() == OrderStatus.INVENTORY_RESERVED) {
            order.transitionTo(OrderStatus.PAYMENT_AUTHORIZED, "Payment authorized by mock gateway");
            order.transitionTo(OrderStatus.COMPLETED, "Fulfillment pipeline verified and completed");
            
            // Commit stock from reserved to finalized deduction
            for (OrderItem item : order.getItems()) {
                inventoryService.commitStock(item.getSku(), item.getQuantity());
            }

            orderRepository.save(order);
            auditService.record("ORDER_COMPLETED", "evt-" + event.getId(), order.getCustomerEmail(),
                    "Order #" + orderId + " completed fulfillment pipeline successfully", null);
            log.info("Order #{} transitioned to COMPLETED and inventory committed.", orderId);
        }
    }

    private void handleOrderCancelled(OutboxEvent event) {
        log.info("Consumer acknowledged cancellation event for aggregate {}", event.getAggregateId());
    }
}
