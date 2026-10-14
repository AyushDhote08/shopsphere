package com.shopsphere.service.impl;

import com.shopsphere.dto.cart.CartItemRequestDTO;
import com.shopsphere.dto.cart.CartItemResponseDTO;
import com.shopsphere.dto.cart.CartItemUpdateDTO;
import com.shopsphere.dto.cart.CartResponseDTO;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.InsufficientStockException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.CartItemRepository;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% tax
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("9.99");
    private static final int MAX_PER_ITEM_QUANTITY = 99;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CartResponseDTO getCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return mapToDTO(cart);
    }

    @Override
    public CartResponseDTO addItemToCart(Long userId, CartItemRequestDTO request) {
        Cart cart = getOrCreateCartEntity(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product '" + product.getName() + "' is currently inactive and cannot be purchased");
        }

        int requestedQty = request.getQuantity() != null ? request.getQuantity() : 1;
        if (requestedQty <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        // Check if item already exists in this customer's cart
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int targetQuantity = requestedQty;
        if (existingItem != null) {
            targetQuantity = existingItem.getQuantity() + requestedQty;
        }

        if (targetQuantity > MAX_PER_ITEM_QUANTITY) {
            throw new BadRequestException("Quantity cannot exceed " + MAX_PER_ITEM_QUANTITY + " units per item");
        }

        if (product.getStockQuantity() < targetQuantity) {
            throw new InsufficientStockException(product.getName(), product.getSku(), targetQuantity, product.getStockQuantity());
        }

        if (existingItem != null) {
            existingItem.setQuantity(targetQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem(cart, product, targetQuantity);
            cartItemRepository.save(newItem);
        }

        return mapToDTO(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Override
    public CartResponseDTO updateCartItemQuantity(Long userId, Long itemId, CartItemUpdateDTO request) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found in your cart with id: '" + itemId + "'"));

        int newQty = request.getQuantity();
        if (newQty <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        if (newQty > MAX_PER_ITEM_QUANTITY) {
            throw new BadRequestException("Quantity cannot exceed " + MAX_PER_ITEM_QUANTITY + " units per item");
        }

        Product product = item.getProduct();
        if (!product.isActive()) {
            throw new BadRequestException("Product '" + product.getName() + "' is no longer available");
        }

        if (product.getStockQuantity() < newQty) {
            throw new InsufficientStockException(product.getName(), product.getSku(), newQty, product.getStockQuantity());
        }

        item.setQuantity(newQty);
        cartItemRepository.save(item);

        return mapToDTO(item.getCart());
    }

    @Override
    public CartResponseDTO removeCartItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found in your cart with id: '" + itemId + "'"));

        Cart cart = item.getCart();
        cartItemRepository.delete(item);

        return mapToDTO(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteAllByCartId(cart.getId());
        }
    }

    private Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            Cart newCart = new Cart(user);
            return cartRepository.save(newCart);
        });
    }

    private CartResponseDTO mapToDTO(Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItemsCount = 0;

        for (CartItem ci : items) {
            Product p = ci.getProduct();
            BigDecimal unitPrice = p.getSellingPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineTotal);
            totalItemsCount += ci.getQuantity();

            CartItemResponseDTO itemDTO = new CartItemResponseDTO();
            itemDTO.setId(ci.getId());
            itemDTO.setProductId(p.getId());
            itemDTO.setProductName(p.getName());
            itemDTO.setProductSku(p.getSku());
            itemDTO.setProductImageUrl(p.getImageUrl());
            itemDTO.setUnitPrice(unitPrice);
            itemDTO.setQuantity(ci.getQuantity());
            itemDTO.setLineTotal(lineTotal);
            itemDTO.setAvailableStock(p.getStockQuantity());
            itemDTO.setInStock(p.isActive() && p.getStockQuantity() >= ci.getQuantity());

            itemDTOs.add(itemDTO);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal tax = subtotal.compareTo(BigDecimal.ZERO) > 0
                ? subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal shipping = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
            if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0) {
                shipping = STANDARD_SHIPPING_FEE;
            }
        }

        BigDecimal total = subtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);

        dto.setItems(itemDTOs);
        dto.setTotalItems(totalItemsCount);
        dto.setSubtotal(subtotal);
        dto.setTaxAmount(tax);
        dto.setShippingFee(shipping);
        dto.setTotalAmount(total);

        return dto;
    }
}
