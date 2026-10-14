package com.shopsphere.service;

import com.shopsphere.dto.admin.RestockRequestDTO;
import com.shopsphere.dto.admin.StockAdjustmentRequestDTO;
import com.shopsphere.dto.admin.UpdateReorderThresholdDTO;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.InsufficientStockException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.InventoryAuditLogRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.impl.AdminInventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryAuditLogRepository inventoryAuditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminInventoryServiceImpl adminInventoryService;

    private Product product;
    private User adminUser;

    @BeforeEach
    void setUp() {
        Category category = new Category("Electronics", "electronics", "Tech Gadgets", null);
        category.setId(10L);

        product = new Product(
                "SKU-TEST-001", "Mechanical Keyboard", "mechanical-keyboard", "RGB backlit keyboard",
                new BigDecimal("45.00"), new BigDecimal("89.99"), 15, 5,
                "https://example.com/kb.jpg", category
        );
        product.setId(100L);

        adminUser = new User("admin@shopsphere.com", "hash", "Admin Officer", "+199988877", Role.ROLE_ADMIN);
        adminUser.setId(1L);
    }

    @Test
    @DisplayName("restockProduct: Successfully restocks product with pessimistic lock and creates audit log")
    void shouldRestockProductSuccessfully() {
        RestockRequestDTO request = new RestockRequestDTO(100L, 25, "Supplier replenishment batch #882");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(productRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = adminInventoryService.restockProduct(1L, request);

        assertThat(response).isNotNull();
        assertThat(product.getStockQuantity()).isEqualTo(40); // 15 + 25 = 40
        assertThat(response.getStockQuantity()).isEqualTo(40);

        ArgumentCaptor<InventoryAuditLog> logCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(logCaptor.capture());

        InventoryAuditLog log = logCaptor.getValue();
        assertThat(log.getProduct().getId()).isEqualTo(100L);
        assertThat(log.getMovementType()).isEqualTo("RESTOCK");
        assertThat(log.getQuantityChanged()).isEqualTo(25);
        assertThat(log.getStockBefore()).isEqualTo(15);
        assertThat(log.getStockAfter()).isEqualTo(40);
        assertThat(log.getReason()).contains("Supplier replenishment batch #882");
        assertThat(log.getUser().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("restockProduct: Rejects restock with zero or negative quantity")
    void shouldRejectZeroOrNegativeRestockQuantity() {
        RestockRequestDTO requestZero = new RestockRequestDTO(100L, 0, "No change");
        RestockRequestDTO requestNeg = new RestockRequestDTO(100L, -5, "Negative restock");

        assertThatThrownBy(() -> adminInventoryService.restockProduct(1L, requestZero))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Restock quantity must be strictly greater than 0");

        assertThatThrownBy(() -> adminInventoryService.restockProduct(1L, requestNeg))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Restock quantity must be strictly greater than 0");

        verify(productRepository, never()).save(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("restockProduct: Throws ResourceNotFoundException if product does not exist")
    void shouldThrowIfProductNotFoundOnRestock() {
        RestockRequestDTO request = new RestockRequestDTO(999L, 10, "Restock non-existent");

        when(productRepository.findByIdWithPessimisticLock(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminInventoryService.restockProduct(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: '999'");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("adjustStock: Successfully applies positive adjustment")
    void shouldApplyPositiveAdjustmentSuccessfully() {
        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO(
                100L, 5, "Found extra units during warehouse count"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(productRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = adminInventoryService.adjustStock(1L, request);

        assertThat(response).isNotNull();
        assertThat(product.getStockQuantity()).isEqualTo(20); // 15 + 5 = 20
        assertThat(response.getStockQuantity()).isEqualTo(20);

        ArgumentCaptor<InventoryAuditLog> logCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(logCaptor.capture());

        InventoryAuditLog log = logCaptor.getValue();
        assertThat(log.getMovementType()).isEqualTo("MANUAL_ADJUSTMENT");
        assertThat(log.getQuantityChanged()).isEqualTo(5);
        assertThat(log.getStockBefore()).isEqualTo(15);
        assertThat(log.getStockAfter()).isEqualTo(20);
        assertThat(log.getReason()).isEqualTo("Found extra units during warehouse count");
    }

    @Test
    @DisplayName("adjustStock: Successfully applies negative adjustment (e.g. damaged stock)")
    void shouldApplyNegativeAdjustmentSuccessfully() {
        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO(
                100L, -3, "Damaged in transit / water leak"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(productRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = adminInventoryService.adjustStock(1L, request);

        assertThat(response).isNotNull();
        assertThat(product.getStockQuantity()).isEqualTo(12); // 15 - 3 = 12
        assertThat(response.getStockQuantity()).isEqualTo(12);

        ArgumentCaptor<InventoryAuditLog> logCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(logCaptor.capture());

        InventoryAuditLog log = logCaptor.getValue();
        assertThat(log.getMovementType()).isEqualTo("MANUAL_ADJUSTMENT");
        assertThat(log.getQuantityChanged()).isEqualTo(-3);
        assertThat(log.getStockBefore()).isEqualTo(15);
        assertThat(log.getStockAfter()).isEqualTo(12);
    }

    @Test
    @DisplayName("adjustStock: Rejects negative adjustment exceeding current stock with BadRequestException")
    void shouldRejectNegativeAdjustmentExceedingStock() {
        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO(
                100L, -20, "Write-off exceeding stock"
        );

        when(productRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> adminInventoryService.adjustStock(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Stock adjustment would result in negative inventory");

        verify(productRepository, never()).save(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("adjustStock: Rejects zero adjustment or blank justification reason")
    void shouldRejectZeroAdjustmentOrBlankReason() {
        StockAdjustmentRequestDTO zeroAdjustment = new StockAdjustmentRequestDTO(100L, 0, "No change");
        StockAdjustmentRequestDTO blankReason = new StockAdjustmentRequestDTO(100L, 5, "   ");

        assertThatThrownBy(() -> adminInventoryService.adjustStock(1L, zeroAdjustment))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Quantity adjustment must not be zero");

        assertThatThrownBy(() -> adminInventoryService.adjustStock(1L, blankReason))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A detailed adjustment reason is mandatory for manual stock changes");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReorderThreshold: Successfully updates reorder threshold")
    void shouldUpdateReorderThresholdSuccessfully() {
        UpdateReorderThresholdDTO request = new UpdateReorderThresholdDTO(10);

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = adminInventoryService.updateReorderThreshold(100L, request);

        assertThat(response).isNotNull();
        assertThat(product.getLowStockThreshold()).isEqualTo(10);
        assertThat(response.getLowStockThreshold()).isEqualTo(10);
    }

    @Test
    @DisplayName("updateReorderThreshold: Rejects negative reorder threshold")
    void shouldRejectNegativeReorderThreshold() {
        UpdateReorderThresholdDTO request = new UpdateReorderThresholdDTO(-1);

        assertThatThrownBy(() -> adminInventoryService.updateReorderThreshold(100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Low stock threshold must be a non-negative number");

        verify(productRepository, never()).save(any());
    }
}
