/**
 * ShopSphere Cart Module
 * Vanilla JavaScript ES6 module for managing customer shopping cart,
 * quantity adjustments, item removals, and real-time server total calculation.
 */
import { api } from './api.js';
import { auth } from './auth.js';

const PLACEHOLDER_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120' viewBox='0 0 120 120'%3E%3Crect width='120' height='120' fill='%23f1f5f9'/%3E%3Ctext x='50%25' y='55%25' text-anchor='middle' fill='%2394a3b8' font-family='system-ui, sans-serif' font-size='11'%3ENo Image%3C/text%3E%3C/svg%3E";

class CartManager {
    constructor() {
        this.cart = null;
    }

    async init() {
        const user = await auth.getCurrentUser();
        const container = document.getElementById('cart-container');

        if (!user) {
            if (container) {
                container.innerHTML = `
                    <div class="empty-state" style="margin-top: 2rem; background: white;">
                        <div class="empty-state-icon">&#128274;</div>
                        <h3>Sign In to View Your Cart</h3>
                        <p>Your shopping cart is securely persisted to your customer account. Please sign in or register to access your saved items.</p>
                        <div style="display: flex; gap: 1rem; justify-content: center; margin-top: 1rem;">
                            <a href="/login.html?redirect=/cart.html" class="btn btn-primary">Sign In</a>
                            <a href="/register.html" class="btn btn-outline">Register Account</a>
                        </div>
                    </div>
                `;
            }
            return;
        }

        await this.loadCart();
    }

    async loadCart() {
        const container = document.getElementById('cart-container');
        if (!container) return;

        try {
            const res = await api.get('/api/cart');
            this.cart = res.data;
            this.renderCart(this.cart);
            auth.updateCartBadge();
        } catch (err) {
            console.error('Failed to load cart:', err);
            container.innerHTML = `
                <div class="empty-state" style="border-color: #fca5a5; background: #fff5f5; margin-top: 2rem;">
                    <div class="empty-state-icon" style="color: var(--danger);">&#9888;</div>
                    <h3>Unable to load cart</h3>
                    <p>${escapeHtml(err.message || 'Error communicating with shopping cart service.')}</p>
                    <button id="btn-retry-cart" class="btn btn-primary btn-sm">Retry</button>
                </div>
            `;
            document.getElementById('btn-retry-cart')?.addEventListener('click', () => this.loadCart());
        }
    }

    renderCart(cart) {
        const container = document.getElementById('cart-container');
        if (!container) return;

        if (!cart || !cart.items || cart.items.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="margin-top: 2rem; background: white;">
                    <div class="empty-state-icon">&#128722;</div>
                    <h3>Your Shopping Cart is Empty</h3>
                    <p>Looks like you haven't added any products to your cart yet. Explore our enterprise catalog to discover high-quality devices.</p>
                    <a href="/index.html" class="btn btn-primary" style="margin-top: 0.5rem;">Explore Catalog &rarr;</a>
                </div>
            `;
            return;
        }

        const itemsHtml = cart.items.map(item => {
            const img = item.productImageUrl || PLACEHOLDER_IMAGE;
            const maxQty = Math.min(item.availableStock, 99);
            const isOutOfStock = !item.inStock;

            let stockAlert = '';
            if (isOutOfStock) {
                stockAlert = '<div style="color: var(--danger); font-size: 0.8rem; font-weight: 600; margin-top: 0.25rem;">&#9888; Out of stock (Only ' + item.availableStock + ' available)</div>';
            }

            return `
                <div class="cart-item-row" data-item-id="${item.id}">
                    <div class="cart-item-img-wrapper">
                        <img src="${img}" alt="${escapeHtml(item.productName)}" class="cart-item-img"
                             onerror="this.onerror=null; this.src='${PLACEHOLDER_IMAGE}'">
                    </div>
                    <div class="cart-item-info">
                        <a href="/product.html?id=${item.productId}" class="cart-item-title">
                            ${escapeHtml(item.productName)}
                        </a>
                        <div class="cart-item-sku">SKU: ${escapeHtml(item.productSku)}</div>
                        <div class="cart-item-price">$${Number(item.unitPrice).toFixed(2)} each</div>
                        ${stockAlert}
                    </div>
                    <div class="cart-item-qty">
                        <div class="quantity-stepper">
                            <button class="qty-btn btn-cart-minus" data-item-id="${item.id}" data-current-qty="${item.quantity}" ${item.quantity <= 1 ? 'disabled' : ''}>&minus;</button>
                            <span class="qty-display">${item.quantity}</span>
                            <button class="qty-btn btn-cart-plus" data-item-id="${item.id}" data-current-qty="${item.quantity}" ${item.quantity >= maxQty ? 'disabled' : ''}>&plus;</button>
                        </div>
                    </div>
                    <div class="cart-item-total">
                        <span style="font-weight: 800; font-size: 1.1rem; color: var(--text-primary);">$${Number(item.lineTotal).toFixed(2)}</span>
                    </div>
                    <div class="cart-item-remove">
                        <button class="btn-remove-item" data-item-id="${item.id}" title="Remove item">&times;</button>
                    </div>
                </div>
            `;
        }).join('');

        const subtotal = Number(cart.subtotal).toFixed(2);
        const tax = Number(cart.taxAmount).toFixed(2);
        const shipping = Number(cart.shippingFee).toFixed(2);
        const total = Number(cart.totalAmount).toFixed(2);

        container.innerHTML = `
            <div class="cart-layout">
                <!-- Items Column -->
                <div class="cart-items-card">
                    <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 1.5rem; border-bottom: 1px solid var(--border-color); padding-bottom: 1rem;">
                        <h2 style="font-size: 1.35rem; font-weight: 700;">Shopping Cart (${cart.totalItems} items)</h2>
                        <button id="btn-clear-cart" class="clear-all-btn">Clear Cart</button>
                    </div>
                    <div class="cart-items-list">
                        ${itemsHtml}
                    </div>
                </div>

                <!-- Order Summary Column -->
                <div class="cart-summary-card">
                    <h2 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 1.25rem;">Order Summary</h2>
                    
                    <div class="summary-line">
                        <span>Items Subtotal</span>
                        <span>$${subtotal}</span>
                    </div>
                    <div class="summary-line">
                        <span>Estimated Tax (5%)</span>
                        <span>$${tax}</span>
                    </div>
                    <div class="summary-line">
                        <span>Shipping</span>
                        <span>${Number(shipping) === 0 ? '<strong style="color: var(--success);">FREE</strong>' : `$${shipping}`}</span>
                    </div>

                    ${Number(subtotal) < 100 ? `
                        <div style="font-size: 0.8rem; color: var(--secondary); background: #f0fdfa; padding: 0.5rem; border-radius: var(--radius-sm); margin: 0.5rem 0;">
                            &#128666; Add <strong>$${(100 - Number(subtotal)).toFixed(2)}</strong> more to qualify for <strong>FREE Shipping</strong>!
                        </div>
                    ` : ''}

                    <div class="summary-total-line">
                        <span>Estimated Total</span>
                        <span style="color: var(--primary);">$${total}</span>
                    </div>

                    <a href="/checkout.html" class="btn btn-primary" style="width: 100%; padding: 0.85rem; margin-top: 1.5rem; text-align: center;">
                        Proceed to Checkout &rarr;
                    </a>
                    
                    <a href="/index.html" class="btn btn-outline" style="width: 100%; margin-top: 0.75rem; text-align: center;">
                        &larr; Continue Shopping
                    </a>

                    <div style="margin-top: 1.5rem; font-size: 0.8rem; color: var(--text-muted); text-align: center;">
                        &#128274; Real-time server price calculations with ACID transactional checkout.
                    </div>
                </div>
            </div>
        `;

        this.bindCartActions();
    }

    bindCartActions() {
        // Quantity Decrement
        document.querySelectorAll('.btn-cart-minus').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const itemId = e.currentTarget.dataset.itemId;
                const currentQty = parseInt(e.currentTarget.dataset.currentQty, 10);
                if (currentQty > 1) {
                    await this.updateItemQuantity(itemId, currentQty - 1);
                }
            });
        });

        // Quantity Increment
        document.querySelectorAll('.btn-cart-plus').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const itemId = e.currentTarget.dataset.itemId;
                const currentQty = parseInt(e.currentTarget.dataset.currentQty, 10);
                await this.updateItemQuantity(itemId, currentQty + 1);
            });
        });

        // Remove item
        document.querySelectorAll('.btn-remove-item').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const itemId = e.currentTarget.dataset.itemId;
                await this.removeItem(itemId);
            });
        });

        // Clear cart
        document.getElementById('btn-clear-cart')?.addEventListener('click', async () => {
            if (confirm('Are you sure you want to remove all items from your cart?')) {
                await this.clearCart();
            }
        });
    }

    async updateItemQuantity(itemId, newQty) {
        try {
            const res = await api.patch(`/api/cart/items/${itemId}`, { quantity: newQty });
            this.cart = res.data;
            this.renderCart(this.cart);
            auth.updateCartBadge();
        } catch (err) {
            alert('Failed to update quantity: ' + (err.message || 'Error communicating with server'));
            await this.loadCart();
        }
    }

    async removeItem(itemId) {
        try {
            const res = await api.delete(`/api/cart/items/${itemId}`);
            this.cart = res.data;
            this.renderCart(this.cart);
            auth.updateCartBadge();
        } catch (err) {
            alert('Failed to remove item: ' + (err.message || 'Error communicating with server'));
        }
    }

    async clearCart() {
        try {
            await api.delete('/api/cart/items');
            await this.loadCart();
        } catch (err) {
            alert('Failed to clear cart: ' + (err.message || 'Error communicating with server'));
        }
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, (m) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    }[m]));
}

export const cartManager = new CartManager();
document.addEventListener('DOMContentLoaded', () => {
    cartManager.init();
});
