package com.infosys.commerce.controller;

import com.infosys.commerce.domain.dto.SystemStatusDto;
import com.infosys.commerce.domain.model.AuditLog;
import com.infosys.commerce.domain.model.OutboxEvent;
import com.infosys.commerce.domain.repository.OutboxEventRepository;
import com.infosys.commerce.service.AuditService;
import com.infosys.commerce.service.OutboxPublisherService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final OutboxPublisherService outboxPublisherService;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;

    public SystemController(OutboxPublisherService outboxPublisherService,
                            OutboxEventRepository outboxEventRepository,
                            AuditService auditService) {
        this.outboxPublisherService = outboxPublisherService;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
    }

    @GetMapping("/status")
    public ResponseEntity<SystemStatusDto> getStatus() {
        long pending = outboxEventRepository.countByStatus("PENDING");
        Map<String, String> adapters = Map.of(
                "CatalogGateway", "FakeStoreAPI / REST (Active)",
                "PersistenceLayer", "JPA / Hibernate (H2 WAL in-memory / PostgreSQL ready)",
                "EventOutbox", "Transactional Outbox Pattern (Active)",
                "MessageBroker", "Kafka / In-JVM Event Bus (Active)",
                "Security", "Spring Security 6 + Stateless JWT"
        );

        SystemStatusDto dto = new SystemStatusDto(
                "event-driven-commerce-intelligence-platform",
                "OPERATIONAL",
                "Production / Staging",
                "LIVE_EXTERNAL_DATA (FakeStore API) + Hybrid Cache",
                pending,
                adapters
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<OutboxEvent>> getRecentOutboxEvents(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(outboxPublisherService.getRecentEvents(limit));
    }

    @GetMapping("/dlq")
    public ResponseEntity<List<OutboxEvent>> getDeadLetterQueue() {
        return ResponseEntity.ok(outboxPublisherService.getDeadLetterQueue());
    }

    @PostMapping("/outbox/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchOutbox() {
        int dispatched = outboxPublisherService.dispatchPendingEvents();
        return ResponseEntity.ok(Map.of(
                "dispatchedCount", dispatched,
                "message", "Pending outbox events dispatched successfully"
        ));
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditService.getRecentLogs(page, size));
    }
}
