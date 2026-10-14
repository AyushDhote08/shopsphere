package com.shopsphere.controller;

import com.shopsphere.dto.cart.CartItemRequestDTO;
import com.shopsphere.dto.cart.CartItemUpdateDTO;
import com.shopsphere.dto.cart.CartResponseDTO;
import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.security.SecurityUtils;
import com.shopsphere.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Shopping Cart", description = "Endpoints for managing the authenticated customer's shopping cart")
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    public CartController(CartService cartService, SecurityUtils securityUtils) {
        this.cartService = cartService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get current cart", description = "Retrieves the authenticated customer's cart with server-recalculated totals")
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart() {
        Long userId = securityUtils.getCurrentUserId();
        CartResponseDTO cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the cart or increments existing quantity with stock validation")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addItem(@Valid @RequestBody CartItemRequestDTO request) {
        Long userId = securityUtils.getCurrentUserId();
        CartResponseDTO cart = cartService.addItemToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity", description = "Updates quantity for a specific cart item belonging to the authenticated user")
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateDTO request) {
        Long userId = securityUtils.getCurrentUserId();
        CartResponseDTO cart = cartService.updateCartItemQuantity(userId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove cart item", description = "Removes a specific cart item belonging to the authenticated user")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeItem(@PathVariable Long itemId) {
        Long userId = securityUtils.getCurrentUserId();
        CartResponseDTO cart = cartService.removeCartItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Clear cart", description = "Removes all items from the authenticated user's cart")
    public ResponseEntity<ApiResponse<String>> clearCart() {
        Long userId = securityUtils.getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", "All items removed"));
    }
}
