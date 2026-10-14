/**
 * ShopSphere Product Detail Module
 * Vanilla JavaScript ES6 module for loading and interacting with
 * full product details, specifications table, and quantity management.
 */
import { api } from './api.js';

const PLACEHOLDER_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='450' viewBox='0 0 600 450'%3E%3Crect width='600' height='450' fill='%23f1f5f9'/%3E%3Cpath d='M240 270l45-60 37 45 53-67 60 82H165z' fill='%23cbd5e1'/%3E%3Ccircle cx='225' cy='165' r='30' fill='%23cbd5e1'/%3E%3Ctext x='50%25' y='80%25' text-anchor='middle' fill='%2394a3b8' font-family='system-ui, sans-serif' font-size='18'%3ENo Image Available%3C/text%3E%3C/svg%3E";

class ProductDetailManager {
    constructor() {
        this.productId = null;
        this.product = null;
        this.quantity = 1;
    }

    async init() {
        const params = new URLSearchParams(window.location.search);
        this.productId = params.get('id');

        if (!this.productId) {
            this.renderError('Product Not Found', 'No product identifier was provided in the URL. Please return to the catalog.');
            return;
        }

        await this.loadProductDetails();
    }

    async loadProductDetails() {
        const container = document.getElementById('product-detail-container');
        try {
            const response = await api.get(`/api/products/${this.productId}`);
            this.product = response.data;
            this.renderProduct(this.product);
        } catch (err) {
            console.error('Failed to load product details:', err);
            const status = err.status || 404;
            const message = err.message || 'The requested product could not be found or has been deactivated.';
            this.renderError(`Product Error (${status})`, message);
        }
    }

    renderProduct(p) {
        // Update browser document title
        document.title = `${p.name} — ShopSphere`;

        // Update Breadcrumbs
        const breadcrumbCat = document.getElementById('breadcrumb-category');
        const breadcrumbProd = document.getElementById('breadcrumb-product');
        if (breadcrumbCat) {
            breadcrumbCat.textContent = p.categoryName || 'Catalog';
            if (p.categoryId) {
                breadcrumbCat.href = `/index.html?categoryId=${p.categoryId}`;
            }
        }
        if (breadcrumbProd) {
            breadcrumbProd.textContent = p.name;
        }

        const container = document.getElementById('product-detail-container');
        if (!container) return;

        const price = Number(p.sellingPrice).toFixed(2);
        const imgUrl = p.imageUrl || PLACEHOLDER_IMAGE;

        let stockBadgeHtml = '';
        if (!p.inStock) {
            stockBadgeHtml = '<span class="stock-pill out-of-stock" style="font-size: 0.9rem; padding: 0.35rem 0.75rem;">Out of Stock</span>';
        } else if (p.lowStock) {
            stockBadgeHtml = `<span class="stock-pill low-stock" style="font-size: 0.9rem; padding: 0.35rem 0.75rem;">Low Stock: Only ${p.stockQuantity} left</span>`;
        } else {
            stockBadgeHtml = `<span class="stock-pill in-stock" style="font-size: 0.9rem; padding: 0.35rem 0.75rem;">In Stock (${p.stockQuantity} available)</span>`;
        }

        let lowStockWarning = '';
        if (p.inStock && p.lowStock) {
            lowStockWarning = `
                <div class="alert alert-danger" style="display: block; margin: 1rem 0 0.5rem; background: #fffbeb; border-color: #fde68a; color: #92400e;">
                    <strong>Limited Availability:</strong> There are only ${p.stockQuantity} units left in stock. Order soon to reserve yours.
                </div>
            `;
        }

        const maxQty = p.inStock ? Math.min(p.stockQuantity, 99) : 0;

        container.innerHTML = `
            <div class="product-detail-layout">
                <!-- Gallery Column -->
                <div class="detail-gallery">
                    <img src="${imgUrl}" 
                         alt="${escapeHtml(p.name)}" 
                         class="detail-image"
                         onerror="this.onerror=null; this.src='${PLACEHOLDER_IMAGE}'">
                </div>

                <!-- Info Column -->
                <div class="detail-info">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span class="product-category" style="font-size: 0.85rem; letter-spacing: 0.5px;">
                            ${escapeHtml(p.categoryName || 'General')}
                        </span>
                        <span style="font-family: monospace; font-size: 0.85rem; color: var(--text-muted);">
                            SKU: ${escapeHtml(p.sku)}
                        </span>
                    </div>

                    <h1 class="detail-title">${escapeHtml(p.name)}</h1>

                    <div style="display: flex; align-items: baseline; gap: 1.25rem;">
                        <span class="detail-price">$${price}</span>
                        ${stockBadgeHtml}
                    </div>

                    ${lowStockWarning}

                    <div style="margin: 1.5rem 0; border-top: 1px solid var(--border-color); padding-top: 1.5rem;">
                        <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);">
                            Product Overview
                        </h3>
                        <p class="detail-desc">
                            ${escapeHtml(p.description || 'No detailed description provided for this catalog item.')}
                        </p>
                    </div>

                    <!-- Quantity & Action -->
                    <div class="quantity-wrapper">
                        <label for="qty-input" style="font-size: 0.95rem; font-weight: 600; color: var(--text-secondary);">
                            Quantity:
                        </label>
                        <div class="quantity-stepper">
                            <button id="btn-qty-minus" class="qty-btn" ${!p.inStock || this.quantity <= 1 ? 'disabled' : ''}>&minus;</button>
                            <input id="qty-input" class="qty-input" type="number" min="1" max="${maxQty}" value="${p.inStock ? 1 : 0}" ${!p.inStock ? 'disabled' : ''} readonly>
                            <button id="btn-qty-plus" class="qty-btn" ${!p.inStock || this.quantity >= maxQty ? 'disabled' : ''}>&plus;</button>
                        </div>
                    </div>

                    <div style="display: flex; gap: 1rem; margin-top: 0.5rem; flex-wrap: wrap;">
                        <button id="btn-add-cart" class="btn btn-primary" style="flex: 2; padding: 0.85rem;" ${!p.inStock ? 'disabled' : ''}>
                            ${p.inStock ? 'Add to Cart' : 'Currently Unavailable'}
                        </button>
                        <a href="/index.html" class="btn btn-outline" style="flex: 1; padding: 0.85rem;">
                            &larr; Back to Catalog
                        </a>
                    </div>

                    <div id="cart-simulation-toast" style="display: none; margin-top: 1rem; padding: 0.85rem 1rem; border-radius: var(--radius-sm); background: var(--primary-light); color: var(--primary); font-size: 0.9rem; border: 1px solid #bfdbfe;">
                        &#10003; <strong>${this.quantity} &times; ${escapeHtml(p.name)}</strong> selected! Full shopping cart and checkout persistence will be implemented in Phase 4.
                    </div>

                    <!-- Technical Specifications Table -->
                    <table class="specs-table">
                        <tbody>
                            <tr>
                                <th>Catalog SKU</th>
                                <td><code>${escapeHtml(p.sku)}</code></td>
                            </tr>
                            <tr>
                                <th>Category</th>
                                <td>${escapeHtml(p.categoryName || 'None')}</td>
                            </tr>
                            <tr>
                                <th>Stock Status</th>
                                <td>${p.inStock ? `Available (${p.stockQuantity} units)` : 'Out of Stock'}</td>
                            </tr>
                            <tr>
                                <th>Restock Threshold</th>
                                <td>${p.lowStockThreshold} units</td>
                            </tr>
                            <tr>
                                <th>Catalog Added</th>
                                <td>${new Date(p.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `;

        this.bindDetailActions(maxQty);
    }

    bindDetailActions(maxQty) {
        const minusBtn = document.getElementById('btn-qty-minus');
        const plusBtn = document.getElementById('btn-qty-plus');
        const qtyInput = document.getElementById('qty-input');
        const addCartBtn = document.getElementById('btn-add-cart');
        const toast = document.getElementById('cart-simulation-toast');

        if (minusBtn && plusBtn && qtyInput) {
            minusBtn.addEventListener('click', () => {
                if (this.quantity > 1) {
                    this.quantity--;
                    qtyInput.value = this.quantity;
                    minusBtn.disabled = this.quantity <= 1;
                    plusBtn.disabled = this.quantity >= maxQty;
                }
            });

            plusBtn.addEventListener('click', () => {
                if (this.quantity < maxQty) {
                    this.quantity++;
                    qtyInput.value = this.quantity;
                    minusBtn.disabled = this.quantity <= 1;
                    plusBtn.disabled = this.quantity >= maxQty;
                }
            });
        }

        if (addCartBtn && toast) {
            addCartBtn.addEventListener('click', async () => {
                addCartBtn.disabled = true;
                const originalText = addCartBtn.textContent;
                addCartBtn.textContent = 'Adding to cart...';

                try {
                    await api.post('/api/cart/items', {
                        productId: this.product.id,
                        quantity: this.quantity
                    });

                    toast.style.display = 'block';
                    toast.style.background = 'var(--success-light)';
                    toast.style.color = 'var(--success)';
                    toast.style.borderColor = '#86efac';
                    toast.innerHTML = `
                        &#10003; <strong>${this.quantity} &times; ${escapeHtml(this.product.name)}</strong> added to your cart! 
                        <a href="/cart.html" style="font-weight: 700; color: var(--primary); text-decoration: underline; margin-left: 0.5rem;">
                            View Cart &rarr;
                        </a>
                    `;

                    // Update navbar badge count
                    const badge = document.getElementById('nav-cart-badge');
                    if (badge) {
                        const currentCount = parseInt(badge.textContent, 10) || 0;
                        badge.textContent = currentCount + this.quantity;
                        badge.style.display = 'inline-flex';
                    }
                } catch (err) {
                    if (err.status === 401) {
                        window.location.href = '/login.html?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
                        return;
                    }

                    toast.style.display = 'block';
                    toast.style.background = 'var(--danger-light)';
                    toast.style.color = 'var(--danger)';
                    toast.style.borderColor = '#fca5a5';
                    toast.innerHTML = `&#9888; <strong>Failed to add item:</strong> ${escapeHtml(err.message || 'Error communicating with cart')}`;
                } finally {
                    addCartBtn.disabled = false;
                    addCartBtn.textContent = originalText;
                    toast.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                }
            });
        }
    }

    renderError(title, message) {
        const container = document.getElementById('product-detail-container');
        if (!container) return;

        container.innerHTML = `
            <div class="empty-state" style="border-color: #fca5a5; background: white; margin-top: 2rem;">
                <div class="empty-state-icon" style="color: var(--danger);">&#9888;</div>
                <h3>${escapeHtml(title)}</h3>
                <p>${escapeHtml(message)}</p>
                <a href="/index.html" class="btn btn-primary btn-sm">&larr; Return to Catalog</a>
            </div>
        `;
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

export const productDetail = new ProductDetailManager();
document.addEventListener('DOMContentLoaded', () => {
    productDetail.init();
});
