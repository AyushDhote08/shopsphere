package com.shopsphere.repository;

import com.shopsphere.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQuantity <= p.lowStockThreshold")
    Page<Product> findLowStockProducts(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.stockQuantity <= p.lowStockThreshold")
    long countLowStockProducts();

    @Query("SELECT coalesce(SUM(p.stockQuantity * p.costPrice), 0.0) FROM Product p WHERE p.isActive = true")
    java.math.BigDecimal calculateTotalCostValue();

    @Query("SELECT coalesce(SUM(p.stockQuantity * p.sellingPrice), 0.0) FROM Product p WHERE p.isActive = true")
    java.math.BigDecimal calculateTotalRetailValue();

    @Query("SELECT coalesce(SUM(p.stockQuantity), 0) FROM Product p WHERE p.isActive = true")
    long calculateTotalUnitsInStock();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdInWithPessimisticLock(@Param("ids") List<Long> ids);
}
