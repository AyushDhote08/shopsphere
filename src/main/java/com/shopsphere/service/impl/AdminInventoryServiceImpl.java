package com.shopsphere.service.impl;

import com.shopsphere.dto.admin.InventoryMovementResponseDTO;
import com.shopsphere.dto.admin.RestockRequestDTO;
import com.shopsphere.dto.admin.StockAdjustmentRequestDTO;
import com.shopsphere.dto.admin.UpdateReorderThresholdDTO;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.entity.InventoryAuditLog;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.InventoryAuditLogRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.AdminInventoryService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AdminInventoryServiceImpl implements AdminInventoryService {

    private final ProductRepository productRepository;
    private final InventoryAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AdminInventoryServiceImpl(ProductRepository productRepository,
                                    InventoryAuditLogRepository auditLogRepository,
                                    UserRepository userRepository) {
        this.productRepository = productRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponseDTO> getProductsInventory(int page, int size, String query, Boolean lowStockOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        Specification<Product> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate skuMatch = cb.like(cb.lower(root.get("sku")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameMatch, skuMatch, descMatch));
            }

            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("lowStockThreshold")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return PageResponse.from(productPage.map(this::mapProductToDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponseDTO> getLowStockProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("stockQuantity").ascending());
        Page<Product> lowStockPage = productRepository.findLowStockProducts(pageable);
        return PageResponse.from(lowStockPage.map(this::mapProductToDTO));
    }

    @Override
    public ProductResponseDTO updateReorderThreshold(Long productId, UpdateReorderThresholdDTO request) {
        if (request.getLowStockThreshold() == null || request.getLowStockThreshold() < 0) {
            throw new BadRequestException("Low stock threshold must be a non-negative number");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        product.setLowStockThreshold(request.getLowStockThreshold());
        Product saved = productRepository.save(product);
        return mapProductToDTO(saved);
    }

    @Override
    public ProductResponseDTO restockProduct(Long adminUserId, RestockRequestDTO request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Restock quantity must be strictly greater than 0");
        }

        // Concurrency-safe: lock product row using pessimistic write lock
        Product product = productRepository.findByIdWithPessimisticLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        User adminUser = null;
        if (adminUserId != null) {
            adminUser = userRepository.findById(adminUserId).orElse(null);
        }

        int stockBefore = product.getStockQuantity();
        int stockAfter = stockBefore + request.getQuantity();

        product.setStockQuantity(stockAfter);
        Product savedProduct = productRepository.save(product);

        String reason = (request.getReason() != null && !request.getReason().trim().isEmpty())
                ? request.getReason().trim()
                : "Supplier Restock";

        InventoryAuditLog auditLog = new InventoryAuditLog(
                savedProduct,
                stockBefore,
                request.getQuantity(),
                stockAfter,
                "RESTOCK",
                request.getReferenceId(),
                reason,
                adminUser
        );
        auditLogRepository.save(auditLog);

        return mapProductToDTO(savedProduct);
    }

    @Override
    public ProductResponseDTO adjustStock(Long adminUserId, StockAdjustmentRequestDTO request) {
        if (request.getQuantityAdjustment() == null || request.getQuantityAdjustment() == 0) {
            throw new BadRequestException("Quantity adjustment must not be zero");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("A detailed adjustment reason is mandatory for manual stock changes");
        }

        // Concurrency-safe: lock product row
        Product product = productRepository.findByIdWithPessimisticLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        User adminUser = null;
        if (adminUserId != null) {
            adminUser = userRepository.findById(adminUserId).orElse(null);
        }

        int stockBefore = product.getStockQuantity();
        int stockAfter = stockBefore + request.getQuantityAdjustment();

        if (stockAfter < 0) {
            throw new BadRequestException("Stock adjustment would result in negative inventory (" + stockAfter
                    + "). Current stock: " + stockBefore + ", requested adjustment: " + request.getQuantityAdjustment());
        }

        product.setStockQuantity(stockAfter);
        Product savedProduct = productRepository.save(product);

        InventoryAuditLog auditLog = new InventoryAuditLog(
                savedProduct,
                stockBefore,
                request.getQuantityAdjustment(),
                stockAfter,
                "MANUAL_ADJUSTMENT",
                request.getReferenceId(),
                request.getReason().trim(),
                adminUser
        );
        auditLogRepository.save(auditLog);

        return mapProductToDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryMovementResponseDTO> getInventoryMovements(Long productId, String movementType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Specification<InventoryAuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (movementType != null && !movementType.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("movementType")), movementType.trim().toUpperCase()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InventoryAuditLog> auditPage = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(auditPage.map(this::mapMovementToDTO));
    }

    private ProductResponseDTO mapProductToDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setDescription(product.getDescription());
        dto.setCostPrice(product.getCostPrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setLowStockThreshold(product.getLowStockThreshold());
        dto.setImageUrl(product.getImageUrl());
        dto.setActive(product.isActive());
        dto.setInStock(product.isInStock());
        dto.setLowStock(product.isLowStock());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        return dto;
    }

    private InventoryMovementResponseDTO mapMovementToDTO(InventoryAuditLog log) {
        InventoryMovementResponseDTO dto = new InventoryMovementResponseDTO();
        dto.setId(log.getId());
        if (log.getProduct() != null) {
            dto.setProductId(log.getProduct().getId());
            dto.setProductName(log.getProduct().getName());
            dto.setProductSku(log.getProduct().getSku());
        }
        dto.setStockBefore(log.getStockBefore());
        dto.setQuantityChanged(log.getQuantityChanged());
        dto.setStockAfter(log.getStockAfter());
        dto.setMovementType(log.getMovementType());
        dto.setReferenceId(log.getReferenceId());
        dto.setReason(log.getReason());

        if (log.getUser() != null) {
            dto.setPerformedBy(log.getUser().getFullName() + " (" + log.getUser().getEmail() + ")");
        } else {
            dto.setPerformedBy("System / Order Placement");
        }

        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
