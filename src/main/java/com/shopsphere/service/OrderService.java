package com.shopsphere.service;

import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.order.CheckoutRequestDTO;
import com.shopsphere.dto.order.OrderResponseDTO;

public interface OrderService {

    OrderResponseDTO checkout(Long userId, CheckoutRequestDTO request);

    PageResponse<OrderResponseDTO> getMyOrders(Long userId, int page, int size);

    OrderResponseDTO getOrderById(Long userId, Long orderId);

    OrderResponseDTO getOrderByOrderNumber(Long userId, String orderNumber);
}
