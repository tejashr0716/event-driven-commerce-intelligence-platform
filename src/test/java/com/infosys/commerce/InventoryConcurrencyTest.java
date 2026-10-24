package com.infosys.commerce;

import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.domain.repository.InventoryRepository;
import com.infosys.commerce.exception.InsufficientStockException;
import com.infosys.commerce.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Pessimistic locking prevents overselling under 20 concurrent race conditions for 5 items")
    public void testHighConcurrencyPessimisticReservation() throws InterruptedException {
        String testSku = "SKU-HOT-CONCURRENCY-TEST";
        int initialStock = 5;
        int totalThreads = 20;

        inventoryService.initializeStock(testSku, initialStock);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Ensure all threads unleash simultaneously
                    inventoryService.reserveStock(testSku, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Fire all threads together
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        Inventory finalInventory = inventoryRepository.findById(testSku).orElseThrow();

        // Strict assertions proving zero overselling
        assertEquals(5, successCount.get(), "Exactly 5 threads must secure the reservation");
        assertEquals(15, rejectedCount.get(), "Exactly 15 threads must receive InsufficientStockException");
        assertEquals(0, finalInventory.getAvailableStock(), "Available stock must be precisely 0");
        assertEquals(5, finalInventory.getReservedStock(), "Reserved stock must be precisely 5");
    }
}
