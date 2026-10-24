package com.infosys.commerce.controller;

import com.infosys.commerce.domain.dto.InventoryResponse;
import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryResponse> getStock(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getInventoryResponse(sku));
    }

    @PostMapping("/restock")
    public ResponseEntity<InventoryResponse> restock(@RequestBody Map<String, Object> payload) {
        String sku = (String) payload.get("sku");
        int quantity = ((Number) payload.get("quantity")).intValue();
        Inventory inv = inventoryService.restock(sku, quantity);
        return ResponseEntity.ok(new InventoryResponse(inv.getSku(), inv.getTotalStock(), inv.getReservedStock(), inv.getAvailableStock()));
    }
}
