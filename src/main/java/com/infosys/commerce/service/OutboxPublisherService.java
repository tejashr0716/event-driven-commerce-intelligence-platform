package com.infosys.commerce.service;

import com.infosys.commerce.domain.model.OutboxEvent;
import com.infosys.commerce.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);
    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final EventConsumerService eventConsumerService;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                  EventConsumerService eventConsumerService) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventConsumerService = eventConsumerService;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void scheduledDispatch() {
        dispatchPendingEvents();
    }

    @Transactional
    public int dispatchPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 50));
        if (pending.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (OutboxEvent event : pending) {
            try {
                boolean success = eventConsumerService.processEvent(event);
                if (success) {
                    event.markProcessed();
                    processedCount++;
                }
            } catch (Exception e) {
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setErrorMessage(e.getMessage());
                if (retries >= MAX_RETRIES) {
                    event.markFailed("Max retries (" + MAX_RETRIES + ") exceeded. Poison pill routed to DLQ: " + e.getMessage());
                    log.error("Outbox Event #{} routed to Dead Letter Queue (DLQ) after {} failures", event.getId(), retries);
                } else {
                    log.warn("Outbox Event #{} dispatch failed (attempt {}): {}", event.getId(), retries, e.getMessage());
                }
            }
            outboxEventRepository.save(event);
        }
        return processedCount;
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getDeadLetterQueue() {
        return outboxEventRepository.findByStatusOrderByCreatedAtAsc("FAILED", PageRequest.of(0, 100));
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getRecentEvents(int limit) {
        return outboxEventRepository.findAll(PageRequest.of(0, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))).getContent();
    }
}
