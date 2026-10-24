package com.infosys.commerce.controller;

import com.infosys.commerce.domain.dto.CreateOrderRequest;
import com.infosys.commerce.domain.dto.OrderResponse;
import com.infosys.commerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String email = authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")
                ? authentication.getName()
                : "customer@infosys.com";

        OrderResponse response = orderService.createOrder(request, idempotencyKey, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, Authentication authentication) {
        String email = authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")
                ? authentication.getName()
                : "customer@infosys.com";

        String role = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority()
                : "ROLE_CUSTOMER";

        return ResponseEntity.ok(orderService.cancelOrder(id, email, role));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        String email = authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")
                ? authentication.getName()
                : "customer@infosys.com";

        return ResponseEntity.ok(orderService.getOrdersForUser(email, page, size));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }
}
