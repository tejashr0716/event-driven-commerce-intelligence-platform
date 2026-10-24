package com.infosys.commerce.domain.repository;

import com.infosys.commerce.domain.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.status = 'PENDING'")
    long countPendingEvents();
}
