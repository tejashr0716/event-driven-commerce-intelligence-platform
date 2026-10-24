package com.infosys.commerce;

import com.infosys.commerce.domain.dto.CreateOrderRequest;
import com.infosys.commerce.domain.dto.OrderItemDto;
import com.infosys.commerce.domain.dto.OrderResponse;
import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.domain.repository.InventoryRepository;
import com.infosys.commerce.domain.repository.OrderRepository;
import com.infosys.commerce.service.InventoryService;
import com.infosys.commerce.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class IdempotencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Idempotent replay returns cached response without duplicating orders or deducting extra stock")
    public void testIdempotentOrderSubmission() {
        String testSku = "SKU-FS-001";
        inventoryService.initializeStock(testSku, 50);

        String idempotencyKey = "TEST-IDEMP-" + UUID.randomUUID();
        String customerEmail = "customer@infosys.com";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemDto(testSku, 2, new BigDecimal("109.95"), "Fjallraven Backpack")),
                "Infosys Electronic City Campus, Bengaluru",
                "USD"
        );

        // First Submission
        OrderResponse firstResponse = orderService.createOrder(request, idempotencyKey, customerEmail);
        assertNotNull(firstResponse);
        assertNotNull(firstResponse.id());

        Inventory invAfterFirst = inventoryRepository.findById(testSku).orElseThrow();
        int availableAfterFirst = invAfterFirst.getAvailableStock();
        assertEquals(48, availableAfterFirst, "Stock should be decremented from 50 to 48");

        // Second Submission with the EXACT SAME Idempotency Key
        OrderResponse secondResponse = orderService.createOrder(request, idempotencyKey, customerEmail);
        assertNotNull(secondResponse);

        // Verification of Idempotency Guarantees
        assertEquals(firstResponse.id(), secondResponse.id(), "Must return identical Order ID");
        assertEquals(firstResponse.totalAmount(), secondResponse.totalAmount(), "Must match total amount");
        assertEquals(firstResponse.idempotencyKey(), secondResponse.idempotencyKey(), "Must match idempotency key");

        // Verify no second inventory reservation took place
        Inventory invAfterSecond = inventoryRepository.findById(testSku).orElseThrow();
        assertEquals(48, invAfterSecond.getAvailableStock(), "Available stock must remain 48 (no second deduction)");

        // Verify exactly one order exists in DB with this idempotency key
        assertEquals(1, orderRepository.findAll().stream().filter(o -> idempotencyKey.equals(o.getIdempotencyKey())).count());
    }
}
