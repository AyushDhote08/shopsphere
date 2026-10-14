/**
 * ShopSphere Orders History Module
 * Manages customer order history listing, pagination, and order details modal.
 */
import { api } from './api.js';
import { auth } from './auth.js';

class OrdersManager {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.totalPages = 1;
        this.orders = [];
    }

    async init() {
        const user = await auth.getCurrentUser();
        if (!user) {
            window.location.href = '/login.html?redirect=/orders.html';
            return;
        }

        await this.loadOrders(0);
        this.bindModalEvents();
    }

    async loadOrders(page = 0) {
        this.currentPage = page;
        const container = document.getElementById('orders-container');
        if (!container) return;

        try {
            const res = await api.get(`/api/orders/my?page=${this.currentPage}&size=${this.pageSize}`);
            const pageData = res.data;
            this.orders = pageData.content || [];
            this.totalPages = pageData.totalPages || 1;

            this.renderOrders(this.orders);
            this.renderPagination(pageData);
        } catch (err) {
            console.error('Failed to load orders:', err);
            container.innerHTML = `
                <div class="empty-state" style="margin-top: 2rem; background: white; border-color: #fca5a5;">
                    <div class="empty-state-icon" style="color: var(--danger);">&#9888;</div>
                    <h3>Unable to load orders</h3>
                    <p>${escapeHtml(err.message || 'Error communicating with orders service.')}</p>
                    <button id="btn-retry-orders" class="btn btn-primary btn-sm" style="margin-top: 0.5rem;">Retry</button>
                </div>
            `;
            document.getElementById('btn-retry-orders')?.addEventListener('click', () => this.loadOrders(this.currentPage));
        }
    }

    renderOrders(orders) {
        const container = document.getElementById('orders-container');
        if (!container) return;

        if (!orders || orders.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="margin-top: 2rem; background: white;">
                    <div class="empty-state-icon">&#128230;</div>
                    <h3>No Orders Placed Yet</h3>
                    <p>You haven't placed any orders yet. Discover high-quality products in our catalog and place your first order!</p>
                    <a href="/index.html" class="btn btn-primary" style="margin-top: 0.5rem;">Explore Catalog &rarr;</a>
                </div>
            `;
            return;
        }

        const cardsHtml = orders.map(order => {
            const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            }) : 'Recent';

            const itemsCount = order.items ? order.items.reduce((acc, it) => acc + it.quantity, 0) : 0;
            const itemNames = order.items && order.items.length > 0
                ? order.items.map(it => escapeHtml(it.productName)).slice(0, 2).join(', ') + (order.items.length > 2 ? ` + ${order.items.length - 2} more` : '')
                : 'Items details';

            return `
                <div class="order-card" style="background: white; border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1.5rem; margin-bottom: 1rem; box-shadow: var(--shadow-sm); transition: var(--transition);">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; border-bottom: 1px solid var(--border-color); padding-bottom: 1rem; margin-bottom: 1rem;">
                        <div>
                            <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Order Number</div>
                            <div style="font-weight: 700; font-family: monospace; font-size: 1rem; color: var(--text-primary);">${escapeHtml(order.orderNumber)}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Date Placed</div>
                            <div style="font-weight: 600; font-size: 0.9rem; color: var(--text-secondary);">${dateStr}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Total Amount</div>
                            <div style="font-weight: 800; font-size: 1.1rem; color: var(--primary);">$${Number(order.totalAmount).toFixed(2)}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Status</div>
                            <div style="display: flex; gap: 0.5rem; margin-top: 0.2rem;">
                                <span class="status-badge status-${order.orderStatus ? order.orderStatus.toLowerCase() : 'pending'}">
                                    ${escapeHtml(order.orderStatus || 'CONFIRMED')}
                                </span>
                                <span class="status-badge status-${order.paymentStatus ? order.paymentStatus.toLowerCase() : 'paid'}">
                                    ${escapeHtml(order.paymentStatus || 'PAID')}
                                </span>
                            </div>
                        </div>
                    </div>

                    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                        <div style="font-size: 0.9rem; color: var(--text-secondary);">
                            <span style="font-weight: 600; color: var(--text-primary);">${itemsCount} item${itemsCount === 1 ? '' : 's'}:</span> ${itemNames}
                        </div>
                        <div style="display: flex; gap: 0.75rem;">
                            <a href="/order-confirmation.html?orderNumber=${encodeURIComponent(order.orderNumber)}" class="btn btn-outline btn-sm">
                                View Receipt
                            </a>
                            <button class="btn btn-primary btn-sm btn-view-order-details" data-order-id="${order.id}">
                                Quick Details
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = cardsHtml;

        // Bind click events on "Quick Details" buttons
        document.querySelectorAll('.btn-view-order-details').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const orderId = parseInt(e.currentTarget.dataset.orderId, 10);
                this.openOrderDetailsModal(orderId);
            });
        });
    }

    renderPagination(pageData) {
        const pagContainer = document.getElementById('orders-pagination');
        if (!pagContainer) return;

        if (this.totalPages <= 1) {
            pagContainer.style.display = 'none';
            return;
        }

        pagContainer.style.display = 'flex';
        let buttonsHtml = '';

        // Previous button
        buttonsHtml += `
            <button class="page-btn ${this.currentPage === 0 ? 'disabled' : ''}" 
                    data-page="${this.currentPage - 1}" 
                    ${this.currentPage === 0 ? 'disabled' : ''}>
                &larr; Previous
            </button>
        `;

        // Page buttons
        for (let i = 0; i < this.totalPages; i++) {
            buttonsHtml += `
                <button class="page-btn ${i === this.currentPage ? 'active' : ''}" 
                        data-page="${i}">
                    ${i + 1}
                </button>
            `;
        }

        // Next button
        buttonsHtml += `
            <button class="page-btn ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''}" 
                    data-page="${this.currentPage + 1}" 
                    ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''}>
                Next &rarr;
            </button>
        `;

        pagContainer.innerHTML = buttonsHtml;

        pagContainer.querySelectorAll('.page-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const p = parseInt(e.currentTarget.dataset.page, 10);
                if (!isNaN(p) && p >= 0 && p < this.totalPages && p !== this.currentPage) {
                    this.loadOrders(p);
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                }
            });
        });
    }

    openOrderDetailsModal(orderId) {
        const order = this.orders.find(o => o.id === orderId);
        if (!order) return;

        const modal = document.getElementById('order-modal');
        const modalTitle = document.getElementById('modal-order-title');
        const modalContent = document.getElementById('modal-order-content');
        const receiptLink = document.getElementById('modal-view-receipt-link');

        if (modalTitle) modalTitle.textContent = `Order ${order.orderNumber}`;
        if (receiptLink) receiptLink.href = `/order-confirmation.html?orderNumber=${encodeURIComponent(order.orderNumber)}`;

        const addr = order.shippingAddress;
        const addrText = addr ? `
            ${escapeHtml(addr.recipientName)}, ${escapeHtml(addr.streetAddress)}, ${escapeHtml(addr.city)}, ${escapeHtml(addr.state)} ${escapeHtml(addr.postalCode)} (Phone: ${escapeHtml(addr.phone)})
        ` : 'Address details unavailable';

        const itemsHtml = (order.items || []).map(item => `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0; border-bottom: 1px solid var(--border-color); font-size: 0.9rem;">
                <div>
                    <div style="font-weight: 700; color: var(--text-primary);">${escapeHtml(item.productName)}</div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); font-family: monospace;">SKU: ${escapeHtml(item.productSku)} &bull; $${Number(item.unitPrice).toFixed(2)} &times; ${item.quantity}</div>
                </div>
                <div style="font-weight: 700; color: var(--text-primary);">$${Number(item.lineTotal).toFixed(2)}</div>
            </div>
        `).join('');

        if (modalContent) {
            modalContent.innerHTML = `
                <div style="margin-bottom: 1rem;">
                    <div style="font-size: 0.8rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase;">Shipping Destination</div>
                    <div style="font-size: 0.9rem; color: var(--text-primary); margin-top: 0.25rem;">${addrText}</div>
                </div>

                <div style="margin-bottom: 1.25rem;">
                    <div style="font-size: 0.8rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; margin-bottom: 0.5rem;">Items Snapshot</div>
                    <div style="border-top: 1px solid var(--border-color);">
                        ${itemsHtml}
                    </div>
                </div>

                <div style="background: #f8fafc; border-radius: var(--radius-sm); padding: 1rem; font-size: 0.9rem;">
                    <div class="summary-line">
                        <span>Items Subtotal</span>
                        <span>$${Number(order.subtotalAmount).toFixed(2)}</span>
                    </div>
                    <div class="summary-line">
                        <span>Tax (5%)</span>
                        <span>$${Number(order.taxAmount).toFixed(2)}</span>
                    </div>
                    <div class="summary-line">
                        <span>Shipping</span>
                        <span>${Number(order.shippingFee) === 0 ? '<strong style="color: var(--success);">FREE</strong>' : `$${Number(order.shippingFee).toFixed(2)}`}</span>
                    </div>
                    <div class="summary-total-line">
                        <span>Total Paid</span>
                        <span style="color: var(--primary);">$${Number(order.totalAmount).toFixed(2)}</span>
                    </div>
                </div>
            `;
        }

        if (modal) {
            modal.style.display = 'flex';
            setTimeout(() => modal.classList.add('open'), 10);
        }
    }

    bindModalEvents() {
        const modal = document.getElementById('order-modal');
        const btnClose = document.getElementById('btn-close-modal');
        const btnCloseFooter = document.getElementById('btn-close-modal-footer');

        const closeModal = () => {
            if (modal) {
                modal.classList.remove('open');
                setTimeout(() => { modal.style.display = 'none'; }, 200);
            }
        };

        btnClose?.addEventListener('click', closeModal);
        btnCloseFooter?.addEventListener('click', closeModal);

        modal?.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeModal();
            }
        });
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

export const ordersManager = new OrdersManager();
document.addEventListener('DOMContentLoaded', () => {
    ordersManager.init();
});
