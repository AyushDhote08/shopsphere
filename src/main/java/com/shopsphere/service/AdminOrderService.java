package com.shopsphere.service;

import com.shopsphere.dto.admin.AdminOrderResponseDTO;
import com.shopsphere.dto.admin.AdminOrderStatusUpdateDTO;
import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.entity.OrderStatus;

import java.time.LocalDate;

public interface AdminOrderService {

    PageResponse<AdminOrderResponseDTO> getOrders(OrderStatus status, String orderNumber,
                                                  LocalDate startDate, LocalDate endDate,
                                                  int page, int size);

    AdminOrderResponseDTO getOrderById(Long id);

    AdminOrderResponseDTO updateOrderStatus(Long adminUserId, Long orderId, AdminOrderStatusUpdateDTO request);
}
