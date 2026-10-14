package com.shopsphere.repository;

import com.shopsphere.entity.InventoryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long> {

    List<InventoryAuditLog> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryAuditLog> findByReferenceId(String referenceId);
}
