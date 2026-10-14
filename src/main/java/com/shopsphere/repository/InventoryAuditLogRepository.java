package com.shopsphere.repository;

import com.shopsphere.entity.InventoryAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long>, JpaSpecificationExecutor<InventoryAuditLog> {

    List<InventoryAuditLog> findByProductIdOrderByCreatedAtDesc(Long productId);

    Page<InventoryAuditLog> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<InventoryAuditLog> findByMovementTypeOrderByCreatedAtDesc(String movementType, Pageable pageable);

    Page<InventoryAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<InventoryAuditLog> findByReferenceId(String referenceId);
}
