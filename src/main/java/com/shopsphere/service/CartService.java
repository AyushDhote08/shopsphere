package com.shopsphere.service;

import com.shopsphere.dto.cart.CartItemRequestDTO;
import com.shopsphere.dto.cart.CartItemUpdateDTO;
import com.shopsphere.dto.cart.CartResponseDTO;

public interface CartService {

    CartResponseDTO getCart(Long userId);

    CartResponseDTO addItemToCart(Long userId, CartItemRequestDTO request);

    CartResponseDTO updateCartItemQuantity(Long userId, Long itemId, CartItemUpdateDTO request);

    CartResponseDTO removeCartItem(Long userId, Long itemId);

    void clearCart(Long userId);
}
