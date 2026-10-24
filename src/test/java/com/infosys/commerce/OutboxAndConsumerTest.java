package com.infosys.commerce;

import com.infosys.commerce.domain.dto.CreateOrderRequest;
import com.infosys.commerce.domain.dto.OrderItemDto;
import com.infosys.commerce.domain.dto.OrderResponse;
import com.infosys.commerce.domain.enums.OrderStatus;
import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.domain.model.Order;
import com.infosys.commerce.domain.model.OutboxEvent;
import com.infosys.commerce.domain.repository.InventoryRepository;
import com.infosys.commerce.domain.repository.OrderRepository;
import com.infosys.commerce.domain.repository.OutboxEventRepository;
import com.infosys.commerce.domain.repository.ProcessedEventRepository;
import com.infosys.commerce.service.EventConsumerService;
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
public class OutboxAndConsumerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private EventConsumerService eventConsumerService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Transactional Outbox creates event atomically and Consumer completes fulfillment")
    public void testOutboxAndConsumerFulfillment() {
        String testSku = "SKU-FS-002";
        inventoryService.initializeStock(testSku, 40);

        String idempotencyKey = "OUTBOX-TEST-" + UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemDto(testSku, 1, new BigDecimal("22.30"), "Slim Fit T-Shirt")),
                "Infosys Pune DC, Hinjewadi",
                "USD"
        );

        OrderResponse orderResponse = orderService.createOrder(request, idempotencyKey, "customer@infosys.com");

        // 1. Verify Outbox event was created in PENDING status
        List<OutboxEvent> outboxList = outboxEventRepository.findAll().stream()
                .filter(e -> orderResponse.id().toString().equals(e.getAggregateId()))
                .toList();

        assertFalse(outboxList.isEmpty(), "Outbox event must be persisted atomically");
        OutboxEvent event = outboxList.get(0);
        assertEquals("PENDING", event.getStatus(), "Initial status must be PENDING");

        // 2. Dispatch pending events via OutboxPublisher
        int dispatched = outboxPublisherService.dispatchPendingEvents();
        assertTrue(dispatched >= 1, "At least 1 outbox event must be dispatched");

        // 3. Verify event is now PROCESSED
        OutboxEvent processedOutbox = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertEquals("PROCESSED", processedOutbox.getStatus());

        // 4. Verify consumer deduplication log contains this event
        String eventKey = "outbox-" + event.getId();
        assertTrue(processedEventRepository.existsById(eventKey), "Consumer must track processed event ID");

        // 5. Test Consumer Deduplication: Re-dispatching same event must return true without errors
        boolean duplicateHandled = eventConsumerService.processEvent(processedOutbox);
        assertTrue(duplicateHandled, "Duplicate event delivery must be gracefully handled via idempotency");

        // 6. Verify final order status is COMPLETED
        Order finalOrder = orderRepository.findById(orderResponse.id()).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, finalOrder.getStatus());
    }

    @Test
    @DisplayName("Poison pill or excessive amount order triggers payment failure and restores stock")
    public void testPoisonPillRiskFailure() {
        String testSku = "SKU-HOT-ITEM-99";
        inventoryService.initializeStock(testSku, 20);

        // Order 10 laptops = $12,999.90 (exceeds $10,000 risk threshold)
        String idempotencyKey = "POISON-TEST-" + UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemDto(testSku, 10, new BigDecimal("1299.99"), "UltraBook Pro 16")),
                "Infosys Global HQ, Electronics City",
                "USD"
        );

        OrderResponse response = orderService.createOrder(request, idempotencyKey, "customer@infosys.com");
        assertEquals(OrderStatus.INVENTORY_RESERVED, response.status());

        // Available stock dropped from 20 to 10
        Inventory invReserved = inventoryRepository.findById(testSku).orElseThrow();
        assertEquals(10, invReserved.getAvailableStock());

        // Dispatch outbox events -> consumer evaluates risk limit
        outboxPublisherService.dispatchPendingEvents();

        // Verify order transitioned to FAILED
        Order failedOrder = orderRepository.findById(response.id()).orElseThrow();
        assertEquals(OrderStatus.FAILED, failedOrder.getStatus());
        assertTrue(failedOrder.getFailureReason().contains("risk limit"));

        // Verify stock was restored to 20
        Inventory invRestored = inventoryRepository.findById(testSku).orElseThrow();
        assertEquals(20, invRestored.getAvailableStock(), "Stock must be fully released upon payment failure");
    }
}
