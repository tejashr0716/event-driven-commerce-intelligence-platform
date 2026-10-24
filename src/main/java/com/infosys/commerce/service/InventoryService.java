package com.infosys.commerce.service;

import com.infosys.commerce.domain.dto.InventoryResponse;
import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.domain.repository.InventoryRepository;
import com.infosys.commerce.exception.InsufficientStockException;
import com.infosys.commerce.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public Inventory reserveStock(String sku, int quantity) {
        Inventory inventory = inventoryRepository.findWithPessimisticLockBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + sku));

        if (inventory.getAvailableStock() < quantity) {
            log.warn("Stock reservation rejected for SKU={}. Available={}, Requested={}",
                    sku, inventory.getAvailableStock(), quantity);
            throw new InsufficientStockException("Insufficient stock for SKU " + sku + 
                    ". Available: " + inventory.getAvailableStock() + ", requested: " + quantity);
        }

        inventory.reserve(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Reserved {} units for SKU={}. Remaining available={}", quantity, sku, saved.getAvailableStock());
        return saved;
    }

    @Transactional
    public Inventory releaseStock(String sku, int quantity) {
        Inventory inventory = inventoryRepository.findWithPessimisticLockBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + sku));

        inventory.release(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Released {} units for SKU={}. Restored available={}", quantity, sku, saved.getAvailableStock());
        return saved;
    }

    @Transactional
    public Inventory commitStock(String sku, int quantity) {
        Inventory inventory = inventoryRepository.findWithPessimisticLockBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + sku));

        inventory.commitReservation(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Committed reservation for {} units of SKU={}. Total stock now={}", quantity, sku, saved.getTotalStock());
        return saved;
    }

    @Transactional
    public Inventory restock(String sku, int additionalQty) {
        Inventory inventory = inventoryRepository.findWithPessimisticLockBySku(sku)
                .orElseGet(() -> new Inventory(sku, 0));

        inventory.restock(additionalQty);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Restocked SKU={} with +{} units. Total stock now={}", sku, additionalQty, saved.getTotalStock());
        return saved;
    }

    @Transactional(readOnly = true)
    public Inventory getInventory(String sku) {
        return inventoryRepository.findById(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryResponse(String sku) {
        Inventory inv = getInventory(sku);
        return new InventoryResponse(inv.getSku(), inv.getTotalStock(), inv.getReservedStock(), inv.getAvailableStock());
    }

    @Transactional
    public Inventory initializeStock(String sku, int totalStock) {
        Inventory inventory = inventoryRepository.findById(sku).orElse(new Inventory(sku, totalStock));
        inventory.setTotalStock(totalStock);
        inventory.setAvailableStock(totalStock);
        inventory.setReservedStock(0);
        return inventoryRepository.save(inventory);
    }
}
