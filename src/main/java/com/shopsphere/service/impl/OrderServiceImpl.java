package com.shopsphere.service.impl;

import com.shopsphere.dto.address.AddressResponseDTO;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.order.CheckoutRequestDTO;
import com.shopsphere.dto.order.OrderItemResponseDTO;
import com.shopsphere.dto.order.OrderResponseDTO;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.InsufficientStockException;
import com.shopsphere.exception.PaymentFailedException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import com.shopsphere.service.AddressService;
import com.shopsphere.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% tax
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("9.99");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final InventoryAuditLogRepository inventoryAuditLogRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            CartRepository cartRepository,
                            CartItemRepository cartItemRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            AddressService addressService,
                            PaymentAttemptRepository paymentAttemptRepository,
                            InventoryAuditLogRepository inventoryAuditLogRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.addressService = addressService;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.inventoryAuditLogRepository = inventoryAuditLogRepository;
    }

    @Override
    public OrderResponseDTO checkout(Long userId, CheckoutRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 1. Idempotency Check: Prevent duplicate submissions
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByUserIdAndIdempotencyKey(
                    userId, request.getIdempotencyKey().trim()
            );
            if (existingOrder.isPresent()) {
                return mapToDTO(existingOrder.get());
            }
        }

        // 2. Fetch Customer Cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No cart found for user"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Cannot checkout with an empty cart");
        }

        // 3. Resolve Shipping Address
        Address shippingAddress = addressService.resolveOrCreateAddress(
                userId, request.getShippingAddressId(), request.getNewShippingAddress()
        );

        // 4. Pessimistic Locking on Contested Products
        // Locks rows using SELECT ... FOR UPDATE in MySQL, preventing race conditions and overselling
        List<Long> productIds = cartItems.stream()
                .map(ci -> ci.getProduct().getId())
                .distinct()
                .collect(Collectors.toList());

        List<Product> lockedProducts = productRepository.findAllByIdInWithPessimisticLock(productIds);
        Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 5. Validate Product Availability, Stock, and Calculate Server Totals
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem ci : cartItems) {
            Product product = productMap.get(ci.getProduct().getId());
            if (product == null || !product.isActive()) {
                String name = product != null ? product.getName() : "ID #" + ci.getProduct().getId();
                throw new BadRequestException("Product '" + name + "' is no longer available in the catalog");
            }

            if (product.getStockQuantity() < ci.getQuantity()) {
                throw new InsufficientStockException(
                        product.getName(), product.getSku(), ci.getQuantity(), product.getStockQuantity()
                );
            }

            BigDecimal lineTotal = product.getSellingPrice()
                    .multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : STANDARD_SHIPPING_FEE;
        BigDecimal total = subtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);

        // 6. Generate Order Number & Transaction Reference
        String orderNumber = "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String txnRef = "TXN-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 7. Simulated Payment Execution
        boolean simulateFailure = "FAILED".equalsIgnoreCase(request.getSimulationOutcome());
        if (simulateFailure) {
            // Simulated payment failed: rollback transaction automatically
            throw new PaymentFailedException(
                    "Simulated payment failed: Card declined or insufficient test balance. Inventory was not deducted.",
                    txnRef
            );
        }

        // 8. Deduct Stock & Create Inventory Audit Logs
        for (CartItem ci : cartItems) {
            Product product = productMap.get(ci.getProduct().getId());
            int oldStock = product.getStockQuantity();
            int newStock = oldStock - ci.getQuantity();
            product.setStockQuantity(newStock);
            productRepository.save(product);

            InventoryAuditLog auditLog = new InventoryAuditLog(
                    product,
                    oldStock,
                    -ci.getQuantity(),
                    newStock,
                    "ORDER_DEDUCTION",
                    orderNumber,
                    "Order placed: " + orderNumber,
                    user
            );
            inventoryAuditLogRepository.save(auditLog);
        }

        // 9. Create and Persist Order
        Order order = new Order(
                orderNumber,
                user,
                shippingAddress,
                subtotal,
                tax,
                shipping,
                total
        );
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            order.setIdempotencyKey(request.getIdempotencyKey().trim());
        }

        Order savedOrder = orderRepository.save(order);

        // 10. Create Immutable OrderItem Snapshots
        for (CartItem ci : cartItems) {
            Product product = productMap.get(ci.getProduct().getId());
            BigDecimal unitPrice = product.getSellingPrice();
            BigDecimal unitCost = product.getCostPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    product,
                    product.getName(),
                    product.getSku(),
                    unitPrice,
                    unitCost,
                    ci.getQuantity(),
                    lineTotal
            );
            savedOrder.addItem(orderItem);
        }
        savedOrder = orderRepository.save(savedOrder);

        // 11. Record Successful Payment Attempt
        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.SIMULATED_CARD;
        PaymentAttempt paymentAttempt = new PaymentAttempt(
                savedOrder,
                method,
                txnRef,
                total,
                PaymentStatus.PAID,
                null
        );
        paymentAttemptRepository.save(paymentAttempt);

        // 12. Clear Customer's Cart after successful order creation
        cartItemRepository.deleteAllByCartId(cart.getId());

        return mapToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponseDTO> getMyOrders(Long userId, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = (size <= 0 || size > 50) ? 10 : size;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<OrderResponseDTO> content = orderPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isFirst(),
                orderPage.isLast(),
                orderPage.hasNext(),
                orderPage.hasPrevious()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByOrderNumber(Long userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order", "orderNumber", orderNumber);
        }

        return mapToDTO(order);
    }

    private OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setSubtotalAmount(order.getSubtotalAmount());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setShippingFee(order.getShippingFee());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getShippingAddress() != null) {
            Address addr = order.getShippingAddress();
            AddressResponseDTO addrDTO = new AddressResponseDTO();
            addrDTO.setId(addr.getId());
            addrDTO.setRecipientName(addr.getRecipientName());
            addrDTO.setPhone(addr.getPhone());
            addrDTO.setStreetAddress(addr.getStreetAddress());
            addrDTO.setCity(addr.getCity());
            addrDTO.setState(addr.getState());
            addrDTO.setPostalCode(addr.getPostalCode());
            addrDTO.setCountry(addr.getCountry());
            addrDTO.setDefault(addr.isDefault());
            dto.setShippingAddress(addrDTO);
        }

        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(item -> new OrderItemResponseDTO(
                        item.getId(),
                        item.getProduct() != null ? item.getProduct().getId() : null,
                        item.getProductNameSnapshot(),
                        item.getProductSkuSnapshot(),
                        item.getUnitPriceSnapshot(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }
}
