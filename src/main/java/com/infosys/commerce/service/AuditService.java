package com.infosys.commerce.service;

import com.infosys.commerce.domain.model.AuditLog;
import com.infosys.commerce.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String correlationId, String actorEmail, String details, String ipAddress) {
        try {
            String corrId = correlationId != null ? correlationId : UUID.randomUUID().toString();
            String actor = actorEmail != null ? actorEmail : "system";
            AuditLog auditLog = new AuditLog(corrId, actor, actor, action, corrId, "SUCCESS", details);
            auditLogRepository.save(auditLog);
            log.info("[AUDIT] Action={}, Actor={}, CorrId={}", action, actor, corrId);
        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getRecentLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findAll(pageable);
    }
}
