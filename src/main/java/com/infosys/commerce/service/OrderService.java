package com.infosys.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.commerce.domain.dto.CreateOrderRequest;
import com.infosys.commerce.domain.dto.OrderItemDto;
import com.infosys.commerce.domain.dto.OrderResponse;
import com.infosys.commerce.domain.enums.EventType;
import com.infosys.commerce.domain.enums.OrderStatus;
import com.infosys.commerce.domain.model.*;
import com.infosys.commerce.domain.repository.IdempotencyRecordRepository;
import com.infosys.commerce.domain.repository.OrderRepository;
import com.infosys.commerce.domain.repository.OutboxEventRepository;
import com.infosys.commerce.domain.repository.ProductRepository;
import com.infosys.commerce.exception.InvalidOrderStateException;
import com.infosys.commerce.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        InventoryService inventoryService,
                        OutboxEventRepository outboxEventRepository,
                        IdempotencyRecordRepository idempotencyRecordRepository,
                        AuthService authService,
                        AuditService auditService,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.outboxEventRepository = outboxEventRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey, String userEmail) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        // 1. Check Idempotency Store
        Optional<IdempotencyRecord> existingRecord = idempotencyRecordRepository.findById(idempotencyKey);
        if (existingRecord.isPresent()) {
            log.info("Idempotent replay detected for key={}. Returning cached response.", idempotencyKey);
            try {
                return objectMapper.readValue(existingRecord.get().getResponseBody(), OrderResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached idempotency payload. Falling back to DB lookup.", e);
                return orderRepository.findByIdempotencyKey(idempotencyKey)
                        .map(this::toResponse)
                        .orElseThrow(() -> new IllegalStateException("Corrupt idempotency state"));
            }
        }

        User user = authService.getByEmail(userEmail);

        // 2. Instantiate and lock items
        Order order = new Order(
                user.getId(),
                user.getEmail(),
                idempotencyKey,
                request.shippingAddress(),
                request.currency() != null ? request.currency() : "USD"
        );

        for (OrderItemDto itemDto : request.items()) {
            Product product = productRepository.findBySku(itemDto.sku())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + itemDto.sku()));

            // Reserve stock with pessimistic lock (throws InsufficientStockException if unavailable)
            inventoryService.reserveStock(itemDto.sku(), itemDto.quantity());

            OrderItem orderItem = new OrderItem(
                    itemDto.sku(),
                    product.getTitle(),
                    product.getPrice(),
                    itemDto.quantity()
            );
            order.addItem(orderItem);
        }

        // Transition from PENDING to INVENTORY_RESERVED
        order.transitionTo(OrderStatus.INVENTORY_RESERVED, "Stock reserved successfully");
        Order savedOrder = orderRepository.save(order);

        // 3. Atomically write Transactional Outbox Event
        String payloadJson;
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("orderId", savedOrder.getId());
            eventPayload.put("userId", savedOrder.getUserId());
            eventPayload.put("totalAmount", savedOrder.getTotalAmount());
            eventPayload.put("currency", savedOrder.getCurrency());
            eventPayload.put("status", savedOrder.getStatus().name());
            eventPayload.put("itemCount", savedOrder.getItems().size());
            payloadJson = objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException e) {
            payloadJson = "{\"orderId\":" + savedOrder.getId() + "}";
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                "Order",
                savedOrder.getId().toString(),
                EventType.ORDER_CREATED,
                payloadJson
        );
        outboxEventRepository.save(outboxEvent);

        OrderResponse response = toResponse(savedOrder);

        // 4. Save Idempotency Record
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            String requestHash = hashRequest(request);
            IdempotencyRecord record = new IdempotencyRecord(
                    idempotencyKey,
                    requestHash,
                    201,
                    responseJson,
                    user.getId()
            );
            idempotencyRecordRepository.save(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency cache", e);
        }

        auditService.record("ORDER_CREATED", idempotencyKey, user.getEmail(),
                "OrderId=" + savedOrder.getId() + ", Total=" + savedOrder.getTotalAmount(), null);

        log.info("Order #{} placed successfully for user={}. Status={}",
                savedOrder.getId(), user.getEmail(), savedOrder.getStatus());

        return response;
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userEmail, String userRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Authorization check: only order owner or admin/manager can cancel
        boolean isOwner = order.getCustomerEmail().equalsIgnoreCase(userEmail);
        boolean isStaff = "ADMIN".equalsIgnoreCase(userRole) || "INVENTORY_MANAGER".equalsIgnoreCase(userRole);
        if (!isOwner && !isStaff) {
            throw new InvalidOrderStateException("Not authorized to cancel order #" + orderId);
        }

        if (order.getStatus() == OrderStatus.CANCELLED ||
            order.getStatus() == OrderStatus.COMPLETED ||
            order.getStatus() == OrderStatus.FAILED) {
            throw new InvalidOrderStateException("Cannot cancel order in terminal status: " + order.getStatus());
        }

        // Release reserved inventory
        for (OrderItem item : order.getItems()) {
            inventoryService.releaseStock(item.getSku(), item.getQuantity());
        }

        order.transitionTo(OrderStatus.CANCELLED, "Cancelled by " + userEmail);
        Order savedOrder = orderRepository.save(order);

        // Write cancellation outbox event
        try {
            Map<String, Object> cancelPayload = new HashMap<>();
            cancelPayload.put("orderId", savedOrder.getId());
            cancelPayload.put("status", OrderStatus.CANCELLED.name());
            cancelPayload.put("cancelledBy", userEmail);
            String cancelJson = objectMapper.writeValueAsString(cancelPayload);
            outboxEventRepository.save(new OutboxEvent("Order", savedOrder.getId().toString(), EventType.ORDER_CANCELLED, cancelJson));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cancellation outbox payload", e);
        }

        auditService.record("ORDER_CANCELLED", UUID.randomUUID().toString(), userEmail,
                "OrderId=" + savedOrder.getId() + " cancelled and inventory restored", null);

        log.info("Order #{} cancelled by {}. Inventory restored.", orderId, userEmail);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersForUser(String userEmail, int page, int size) {
        User user = authService.getByEmail(userEmail);
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateStatusDirectly(Long orderId, OrderStatus newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        order.transitionTo(newStatus, reason);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(item.getSku(), item.getQuantity(), item.getUnitPrice(), item.getTitle()))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getCustomerEmail(),
                order.getIdempotencyKey(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getShippingAddress(),
                order.getFailureReason(),
                itemDtos,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private String hashRequest(CreateOrderRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(request.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(request.hashCode());
        }
    }
}
