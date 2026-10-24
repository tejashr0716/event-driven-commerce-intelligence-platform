package com.infosys.commerce;

import com.infosys.commerce.domain.dto.CreateOrderRequest;
import com.infosys.commerce.domain.dto.OrderItemDto;
import com.infosys.commerce.domain.dto.OrderResponse;
import com.infosys.commerce.domain.enums.OrderStatus;
import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.domain.repository.InventoryRepository;
import com.infosys.commerce.domain.repository.OrderRepository;
import com.infosys.commerce.exception.InvalidOrderStateException;
import com.infosys.commerce.service.InventoryService;
import com.infosys.commerce.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderLifecycleIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Customer order cancellation restores inventory and blocks subsequent state changes")
    public void testCustomerCancellationAndInventoryRestoration() {
        String testSku = "SKU-FS-003";
        inventoryService.initializeStock(testSku, 25);

        String idempotencyKey = "CANCEL-TEST-" + UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemDto(testSku, 3, new BigDecimal("55.99"), "Mens Cotton Jacket")),
                "Infosys Hyderabad SEZ, Gachibowli",
                "USD"
        );

        OrderResponse order = orderService.createOrder(request, idempotencyKey, "customer@infosys.com");
        assertEquals(OrderStatus.INVENTORY_RESERVED, order.status());

        // Verify stock deducted
        Inventory afterReserve = inventoryRepository.findById(testSku).orElseThrow();
        assertEquals(22, afterReserve.getAvailableStock());
        assertEquals(3, afterReserve.getReservedStock());

        // Customer cancels order
        OrderResponse cancelled = orderService.cancelOrder(order.id(), "customer@infosys.com", "ROLE_CUSTOMER");
        assertEquals(OrderStatus.CANCELLED, cancelled.status());

        // Verify inventory restored immediately
        Inventory afterCancel = inventoryRepository.findById(testSku).orElseThrow();
        assertEquals(25, afterCancel.getAvailableStock(), "Available stock must be restored to 25");
        assertEquals(0, afterCancel.getReservedStock(), "Reserved stock must be 0");

        // Attempting to cancel an already cancelled order must fail with InvalidOrderStateException
        assertThrows(InvalidOrderStateException.class, () -> {
            orderService.cancelOrder(order.id(), "customer@infosys.com", "ROLE_CUSTOMER");
        });
    }

    @Test
    @DisplayName("Restocking inventory increases total and available stock seamlessly")
    public void testInventoryRestocking() {
        String testSku = "SKU-FS-004";
        inventoryService.initializeStock(testSku, 10);

        Inventory restocked = inventoryService.restock(testSku, 15);
        assertEquals(25, restocked.getTotalStock());
        assertEquals(25, restocked.getAvailableStock());
    }
}
