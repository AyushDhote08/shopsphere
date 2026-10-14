package com.shopsphere.service.impl;

import com.shopsphere.dto.address.AddressResponseDTO;
import com.shopsphere.dto.admin.AdminOrderItemResponseDTO;
import com.shopsphere.dto.admin.AdminOrderResponseDTO;
import com.shopsphere.dto.admin.AdminOrderStatusUpdateDTO;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import com.shopsphere.service.AdminOrderService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryAuditLogRepository auditLogRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final UserRepository userRepository;

    public AdminOrderServiceImpl(OrderRepository orderRepository,
                                ProductRepository productRepository,
                                InventoryAuditLogRepository auditLogRepository,
                                PaymentAttemptRepository paymentAttemptRepository,
                                UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.auditLogRepository = auditLogRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponseDTO> getOrders(OrderStatus status, String orderNumber,
                                                        LocalDate startDate, LocalDate endDate,
                                                        int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("orderStatus"), status));
            }

            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("orderNumber")), "%" + orderNumber.trim().toLowerCase() + "%"));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }

            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return PageResponse.from(orderPage.map(this::mapToAdminDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return mapToAdminDTO(order);
    }

    @Override
    public AdminOrderResponseDTO updateOrderStatus(Long adminUserId, Long orderId, AdminOrderStatusUpdateDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getNewStatus();

        if (currentStatus == newStatus) {
            return mapToAdminDTO(order); // Idempotent no-op
        }

        // Validate state machine transitions
        validateStatusTransition(currentStatus, newStatus);

        User adminUser = null;
        if (adminUserId != null) {
            adminUser = userRepository.findById(adminUserId).orElse(null);
        }

        // If transitioning to CANCELLED from an active inventory-holding state (PENDING or CONFIRMED)
        if (newStatus == OrderStatus.CANCELLED && (currentStatus == OrderStatus.PENDING || currentStatus == OrderStatus.CONFIRMED)) {
            restoreOrderInventory(order, request.getReason(), adminUser);
            order.setCancellationReason(request.getReason() != null ? request.getReason().trim() : "Cancelled by Administrator");
        }

        order.setOrderStatus(newStatus);
        Order saved = orderRepository.save(order);

        return mapToAdminDTO(saved);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        boolean valid = false;

        switch (currentStatus) {
            case PENDING -> valid = (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED);
            case CONFIRMED -> valid = (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED);
            case SHIPPED -> valid = (newStatus == OrderStatus.DELIVERED);
            case DELIVERED, CANCELLED -> valid = false; // Terminal states
        }

        if (!valid) {
            throw new BadRequestException("Invalid order status transition from " + currentStatus + " to " + newStatus);
        }
    }

    private void restoreOrderInventory(Order order, String reason, User adminUser) {
        String logReason = (reason != null && !reason.trim().isEmpty())
                ? "Order cancellation restore: " + reason.trim()
                : "Order cancellation restore: " + order.getOrderNumber();

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Product product = productRepository.findByIdWithPessimisticLock(item.getProduct().getId())
                        .orElse(null);
                if (product != null) {
                    int oldStock = product.getStockQuantity();
                    int newStock = oldStock + item.getQuantity();
                    product.setStockQuantity(newStock);
                    productRepository.save(product);

                    InventoryAuditLog auditLog = new InventoryAuditLog(
                            product,
                            oldStock,
                            item.getQuantity(),
                            newStock,
                            "ORDER_CANCELLED_RESTORE",
                            order.getOrderNumber(),
                            logReason,
                            adminUser
                    );
                    auditLogRepository.save(auditLog);
                }
            }
        }
    }

    private AdminOrderResponseDTO mapToAdminDTO(Order order) {
        AdminOrderResponseDTO dto = new AdminOrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setSubtotalAmount(order.getSubtotalAmount());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setShippingFee(order.getShippingFee());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCancellationReason(order.getCancellationReason());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Customer Info
        if (order.getUser() != null) {
            dto.setCustomerId(order.getUser().getId());
            dto.setCustomerName(order.getUser().getFullName());
            dto.setCustomerEmail(order.getUser().getEmail());
            dto.setCustomerPhone(order.getUser().getPhone());
        }

        // Shipping Address
        if (order.getShippingAddress() != null) {
            Address addr = order.getShippingAddress();
            AddressResponseDTO addrDTO = new AddressResponseDTO(
                    addr.getId(),
                    addr.getRecipientName(),
                    addr.getPhone(),
                    addr.getStreetAddress(),
                    addr.getCity(),
                    addr.getState(),
                    addr.getPostalCode(),
                    addr.getCountry(),
                    addr.isDefault()
            );
            dto.setShippingAddress(addrDTO);
        }

        // Payment details from latest attempt
        paymentAttemptRepository.findTopByOrderIdOrderByAttemptedAtDesc(order.getId())
                .ifPresent(attempt -> {
                    dto.setPaymentMethod(attempt.getPaymentMethod());
                    dto.setTransactionReference(attempt.getTransactionReference());
                });

        // Items and Gross Profit calculation
        BigDecimal totalCost = BigDecimal.ZERO;
        List<AdminOrderItemResponseDTO> itemDTOs = new ArrayList<>();

        for (OrderItem it : order.getItems()) {
            BigDecimal unitPrice = it.getUnitPriceSnapshot();
            BigDecimal unitCost = it.getUnitCostSnapshot() != null ? it.getUnitCostSnapshot() : BigDecimal.ZERO;
            BigDecimal lineTotal = it.getLineTotal();
            BigDecimal itemTotalCost = unitCost.multiply(BigDecimal.valueOf(it.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemProfit = lineTotal.subtract(itemTotalCost).setScale(2, RoundingMode.HALF_UP);

            totalCost = totalCost.add(itemTotalCost);

            AdminOrderItemResponseDTO itemDTO = new AdminOrderItemResponseDTO(
                    it.getId(),
                    it.getProduct() != null ? it.getProduct().getId() : null,
                    it.getProductNameSnapshot(),
                    it.getProductSkuSnapshot(),
                    unitPrice,
                    unitCost,
                    it.getQuantity(),
                    lineTotal,
                    itemProfit
            );
            itemDTOs.add(itemDTO);
        }

        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);
        dto.setItems(itemDTOs);
        dto.setTotalCostAmount(totalCost);
        dto.setGrossProfit(order.getSubtotalAmount().subtract(totalCost).setScale(2, RoundingMode.HALF_UP));

        return dto;
    }
}
