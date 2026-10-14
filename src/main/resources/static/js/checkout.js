/**
 * ShopSphere Checkout Module
 * Handles address selection/creation, simulated payment method selection,
 * idempotency key generation, and transactional checkout execution.
 */
import { api } from './api.js';
import { auth } from './auth.js';

class CheckoutManager {
    constructor() {
        this.cart = null;
        this.addresses = [];
        this.selectedAddressId = null;
        this.isNewAddressMode = false;
        this.idempotencyKey = this.generateIdempotencyKey();
    }

    generateIdempotencyKey() {
        if (typeof crypto !== 'undefined' && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        return 'idemp-' + Date.now() + '-' + Math.random().toString(36).substring(2, 10);
    }

    async init() {
        const user = await auth.getCurrentUser();
        if (!user) {
            window.location.href = '/login.html?redirect=/checkout.html';
            return;
        }

        try {
            const [cartRes, addressRes] = await Promise.all([
                api.get('/api/cart'),
                api.get('/api/addresses')
            ]);

            this.cart = cartRes.data;
            this.addresses = addressRes.data || [];

            if (!this.cart || !this.cart.items || this.cart.items.length === 0) {
                this.renderEmptyCart();
                return;
            }

            // Set default address if available
            if (this.addresses.length > 0) {
                const defaultAddr = this.addresses.find(a => a.isDefault) || this.addresses[0];
                this.selectedAddressId = defaultAddr.id;
                this.isNewAddressMode = false;
            } else {
                this.isNewAddressMode = true;
            }

            this.renderCheckout();
        } catch (err) {
            console.error('Failed to initialize checkout:', err);
            this.showAlert('danger', 'Failed to load checkout data: ' + (err.message || 'Server error'));
        }
    }

    renderEmptyCart() {
        const container = document.getElementById('checkout-main-content');
        if (!container) return;

        container.innerHTML = `
            <div class="empty-state" style="margin-top: 2rem; background: white;">
                <div class="empty-state-icon">&#128722;</div>
                <h3>Your Cart is Empty</h3>
                <p>You cannot proceed to checkout without any items in your shopping cart.</p>
                <div style="display: flex; gap: 1rem; justify-content: center; margin-top: 1rem;">
                    <a href="/index.html" class="btn btn-primary">Browse Catalog</a>
                    <a href="/cart.html" class="btn btn-outline">View Cart</a>
                </div>
            </div>
        `;
    }

    renderCheckout() {
        const container = document.getElementById('checkout-main-content');
        if (!container) return;

        const cart = this.cart;
        const subtotal = Number(cart.subtotal).toFixed(2);
        const tax = Number(cart.taxAmount).toFixed(2);
        const shipping = Number(cart.shippingFee).toFixed(2);
        const total = Number(cart.totalAmount).toFixed(2);

        // Address list HTML
        let addressesHtml = '';
        if (this.addresses.length > 0) {
            addressesHtml = `
                <div class="address-options-list" style="display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1rem;">
                    ${this.addresses.map(addr => `
                        <label class="address-card-label ${this.selectedAddressId === addr.id && !this.isNewAddressMode ? 'selected' : ''}">
                            <input type="radio" name="addressOption" value="${addr.id}" 
                                   ${this.selectedAddressId === addr.id && !this.isNewAddressMode ? 'checked' : ''} 
                                   style="margin-top: 0.25rem;">
                            <div class="address-card-body">
                                <div style="display: flex; align-items: center; gap: 0.5rem;">
                                    <strong>${escapeHtml(addr.recipientName)}</strong>
                                    ${addr.isDefault ? '<span class="badge-subtle">Default</span>' : ''}
                                </div>
                                <div style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.2rem;">
                                    ${escapeHtml(addr.streetAddress)}, ${escapeHtml(addr.city)}, ${escapeHtml(addr.state)} ${escapeHtml(addr.postalCode)}
                                </div>
                                <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">
                                    Phone: ${escapeHtml(addr.phone)} &bull; ${escapeHtml(addr.country)}
                                </div>
                            </div>
                        </label>
                    `).join('')}

                    <label class="address-card-label ${this.isNewAddressMode ? 'selected' : ''}">
                        <input type="radio" name="addressOption" value="new" ${this.isNewAddressMode ? 'checked' : ''} style="margin-top: 0.25rem;">
                        <div class="address-card-body">
                            <strong>+ Add a new delivery address</strong>
                            <div style="font-size: 0.85rem; color: var(--text-secondary);">Enter a new recipient name, destination address, and phone number</div>
                        </div>
                    </label>
                </div>
            `;
        }

        // New Address Form HTML
        const newAddressFormHtml = `
            <div id="new-address-section" style="${this.isNewAddressMode ? 'display: block;' : 'display: none;'} margin-top: 1rem; padding: 1.25rem; background: #f8fafc; border: 1px solid var(--border-color); border-radius: var(--radius-md);">
                <h4 style="font-size: 1rem; margin-bottom: 1rem; color: var(--text-primary);">Enter New Shipping Details</h4>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                    <div class="form-group" style="margin-bottom: 0.75rem;">
                        <label class="form-label" for="addr-recipient">Recipient Full Name *</label>
                        <input type="text" id="addr-recipient" class="form-control" placeholder="e.g. Jane Doe" required>
                    </div>
                    <div class="form-group" style="margin-bottom: 0.75rem;">
                        <label class="form-label" for="addr-phone">Phone Number *</label>
                        <input type="tel" id="addr-phone" class="form-control" placeholder="e.g. +1 555-0199" required>
                    </div>
                </div>

                <div class="form-group" style="margin-bottom: 0.75rem;">
                    <label class="form-label" for="addr-street">Street Address *</label>
                    <input type="text" id="addr-street" class="form-control" placeholder="e.g. 123 Tech Boulevard, Suite 400" required>
                </div>

                <div style="display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 1rem;">
                    <div class="form-group" style="margin-bottom: 0.75rem;">
                        <label class="form-label" for="addr-city">City *</label>
                        <input type="text" id="addr-city" class="form-control" placeholder="e.g. San Francisco" required>
                    </div>
                    <div class="form-group" style="margin-bottom: 0.75rem;">
                        <label class="form-label" for="addr-state">State / Province *</label>
                        <input type="text" id="addr-state" class="form-control" placeholder="e.g. CA" required>
                    </div>
                    <div class="form-group" style="margin-bottom: 0.75rem;">
                        <label class="form-label" for="addr-zip">Postal Code *</label>
                        <input type="text" id="addr-zip" class="form-control" placeholder="e.g. 94105" required>
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; align-items: center; margin-top: 0.5rem;">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label" for="addr-country">Country</label>
                        <input type="text" id="addr-country" class="form-control" value="United States">
                    </div>
                    <div style="padding-top: 1.5rem;">
                        <label style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer; font-size: 0.9rem;">
                            <input type="checkbox" id="addr-default">
                            <span>Save as my default shipping address</span>
                        </label>
                    </div>
                </div>
            </div>
        `;

        // Cart items review list HTML
        const itemsSummaryHtml = cart.items.map(item => `
            <div class="summary-item-row" style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0; border-bottom: 1px solid var(--border-color); font-size: 0.9rem;">
                <div style="display: flex; gap: 0.75rem; align-items: center;">
                    <div style="width: 40px; height: 40px; background: #f1f5f9; border-radius: var(--radius-sm); overflow: hidden; display: flex; align-items: center; justify-content: center; font-size: 0.8rem; color: var(--text-muted); font-weight: 700;">
                        ${item.quantity}x
                    </div>
                    <div>
                        <div style="font-weight: 600; color: var(--text-primary);">${escapeHtml(item.productName)}</div>
                        <div style="font-size: 0.75rem; color: var(--text-muted);">SKU: ${escapeHtml(item.productSku)}</div>
                    </div>
                </div>
                <div style="font-weight: 700; color: var(--text-primary);">$${Number(item.lineTotal).toFixed(2)}</div>
            </div>
        `).join('');

        container.innerHTML = `
            <div class="checkout-grid">
                <!-- Left Form Column -->
                <div class="checkout-form-column">
                    <!-- Step 1: Shipping Address -->
                    <div class="checkout-section-card">
                        <div class="checkout-step-header">
                            <span class="step-num">1</span>
                            <h3>Shipping Destination</h3>
                        </div>
                        <div style="padding-left: 2.25rem;">
                            ${addressesHtml}
                            ${newAddressFormHtml}
                        </div>
                    </div>

                    <!-- Step 2: Payment Simulation -->
                    <div class="checkout-section-card" style="margin-top: 1.5rem;">
                        <div class="checkout-step-header">
                            <span class="step-num">2</span>
                            <h3>Simulated Payment Processing</h3>
                        </div>
                        <div style="padding-left: 2.25rem;">
                            <div class="simulation-banner">
                                <span style="font-size: 1.25rem;">&#128737;</span>
                                <div>
                                    <strong>Development & Interview Mode: Simulated Gateway</strong>
                                    <div style="font-size: 0.8rem; margin-top: 0.2rem; line-height: 1.4;">
                                        ShopSphere executes realistic payment simulation with transactional rollback guarantees.
                                        No real card or banking data is required or transmitted.
                                    </div>
                                </div>
                            </div>

                            <label class="form-label" style="margin-top: 1rem;">Choose Simulated Payment Instrument</label>
                            <div class="payment-methods-grid">
                                <label class="payment-method-card selected">
                                    <input type="radio" name="paymentMethod" value="SIMULATED_CARD" checked>
                                    <div>
                                        <div style="font-weight: 700; font-size: 0.95rem;">&#128179; Credit / Debit Card</div>
                                        <div style="font-size: 0.8rem; color: var(--text-muted);">Instant simulated transaction</div>
                                    </div>
                                </label>
                                <label class="payment-method-card">
                                    <input type="radio" name="paymentMethod" value="SIMULATED_UPI">
                                    <div>
                                        <div style="font-weight: 700; font-size: 0.95rem;">&#128241; Simulated UPI</div>
                                        <div style="font-size: 0.8rem; color: var(--text-muted);">Instant virtual payment address</div>
                                    </div>
                                </label>
                                <label class="payment-method-card">
                                    <input type="radio" name="paymentMethod" value="SIMULATED_NET_BANKING">
                                    <div>
                                        <div style="font-weight: 700; font-size: 0.95rem;">&#127974; Net Banking</div>
                                        <div style="font-size: 0.8rem; color: var(--text-muted);">Simulated institutional portal</div>
                                    </div>
                                </label>
                            </div>

                            <!-- Testing Simulation Outcome -->
                            <div class="simulation-toggle-card" style="margin-top: 1.25rem; background: #fdf2f8; border: 1px dashed #f472b6; border-radius: var(--radius-sm); padding: 1rem;">
                                <div style="font-size: 0.85rem; font-weight: 700; color: #9d174d; margin-bottom: 0.4rem;">
                                    &#128300; Transaction Outcome Simulator (Test ACID Rollback)
                                </div>
                                <div style="font-size: 0.8rem; color: #831843; margin-bottom: 0.75rem;">
                                    Test how the backend handles transaction safety. Selecting <strong>FAILED</strong> triggers simulated rejection:
                                    Spring's <code>@Transactional</code> automatically rolls back, preserving product inventory and retaining your cart items.
                                </div>
                                <div style="display: flex; gap: 1rem;">
                                    <label style="display: flex; align-items: center; gap: 0.4rem; font-size: 0.85rem; cursor: pointer;">
                                        <input type="radio" name="simulationOutcome" value="SUCCESS" checked>
                                        <span style="font-weight: 600; color: var(--success);">&#9989; Simulate SUCCESS</span>
                                    </label>
                                    <label style="display: flex; align-items: center; gap: 0.4rem; font-size: 0.85rem; cursor: pointer;">
                                        <input type="radio" name="simulationOutcome" value="FAILED">
                                        <span style="font-weight: 600; color: var(--danger);">&#10060; Simulate FAILURE (Rollback)</span>
                                    </label>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Right Summary Column -->
                <div class="checkout-summary-column">
                    <div class="cart-summary-card">
                        <h3 style="font-size: 1.2rem; font-weight: 700; margin-bottom: 1rem;">Review Order (${cart.totalItems} items)</h3>
                        
                        <div class="summary-items-scroll" style="max-height: 220px; overflow-y: auto; margin-bottom: 1rem;">
                            ${itemsSummaryHtml}
                        </div>

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
                        <div class="summary-total-line">
                            <span>Order Total</span>
                            <span style="color: var(--primary);">$${total}</span>
                        </div>

                        <button id="btn-submit-order" class="btn btn-primary" style="width: 100%; padding: 0.9rem; font-size: 1rem; margin-top: 1.25rem;">
                            Place Order &bull; $${total}
                        </button>

                        <div style="margin-top: 1rem; font-size: 0.75rem; color: var(--text-muted); line-height: 1.4; text-align: center;">
                            &#128274; <strong>Idempotent Submission</strong> &bull; UUID: <code>${this.idempotencyKey.substring(0, 18)}...</code><br>
                            Concurrency protected via pessimistic row locking (<code>SELECT ... FOR UPDATE</code>).
                        </div>
                    </div>
                </div>
            </div>
        `;

        this.bindEvents();
    }

    bindEvents() {
        // Address radio toggle
        document.querySelectorAll('input[name="addressOption"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                const val = e.target.value;
                const newSection = document.getElementById('new-address-section');
                
                document.querySelectorAll('.address-card-label').forEach(label => label.classList.remove('selected'));
                e.target.closest('.address-card-label').classList.add('selected');

                if (val === 'new') {
                    this.isNewAddressMode = true;
                    this.selectedAddressId = null;
                    if (newSection) newSection.style.display = 'block';
                } else {
                    this.isNewAddressMode = false;
                    this.selectedAddressId = parseInt(val, 10);
                    if (newSection) newSection.style.display = 'none';
                }
            });
        });

        // Payment method radio selection styling
        document.querySelectorAll('input[name="paymentMethod"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                document.querySelectorAll('.payment-method-card').forEach(c => c.classList.remove('selected'));
                e.target.closest('.payment-method-card').classList.add('selected');
            });
        });

        // Submit order button
        const btnSubmit = document.getElementById('btn-submit-order');
        if (btnSubmit) {
            btnSubmit.addEventListener('click', (e) => {
                e.preventDefault();
                this.handleCheckoutSubmit();
            });
        }
    }

    async handleCheckoutSubmit() {
        const btn = document.getElementById('btn-submit-order');
        const alertBox = document.getElementById('checkout-alert');

        // Validation
        let newShippingAddress = null;
        if (this.isNewAddressMode) {
            const recipientName = document.getElementById('addr-recipient')?.value.trim();
            const phone = document.getElementById('addr-phone')?.value.trim();
            const streetAddress = document.getElementById('addr-street')?.value.trim();
            const city = document.getElementById('addr-city')?.value.trim();
            const state = document.getElementById('addr-state')?.value.trim();
            const postalCode = document.getElementById('addr-zip')?.value.trim();
            const country = document.getElementById('addr-country')?.value.trim() || 'United States';
            const isDefault = document.getElementById('addr-default')?.checked || false;

            if (!recipientName || !phone || !streetAddress || !city || !state || !postalCode) {
                this.showAlert('danger', 'Please complete all required shipping address fields.');
                return;
            }

            newShippingAddress = {
                recipientName,
                phone,
                streetAddress,
                city,
                state,
                postalCode,
                country,
                isDefault
            };
        } else if (!this.selectedAddressId) {
            this.showAlert('danger', 'Please select or enter a shipping destination address.');
            return;
        }

        const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'SIMULATED_CARD';
        const simulationOutcome = document.querySelector('input[name="simulationOutcome"]:checked')?.value || 'SUCCESS';

        const payload = {
            paymentMethod,
            simulationOutcome,
            idempotencyKey: this.idempotencyKey
        };

        if (this.isNewAddressMode) {
            payload.newShippingAddress = newShippingAddress;
        } else {
            payload.shippingAddressId = this.selectedAddressId;
        }

        // Disable button to prevent double-clicks
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = `<span class="spinner" style="display: inline-block; width: 16px; height: 16px; border: 2px solid #ffffff; border-top-color: transparent; border-radius: 50%; animation: spin 0.8s linear infinite; vertical-align: middle; margin-right: 0.5rem;"></span> Processing Order...`;
        }
        if (alertBox) alertBox.style.display = 'none';

        try {
            const res = await api.post('/api/orders/checkout', payload);
            if (res && res.data && res.data.orderNumber) {
                // Update badge and redirect
                await auth.updateCartBadge();
                window.location.href = `/order-confirmation.html?orderNumber=${res.data.orderNumber}`;
            } else {
                throw new Error('Order confirmation details not received from server.');
            }
        } catch (err) {
            console.error('Checkout error:', err);
            const msg = err.message || 'Payment simulation failed or inventory unavailable.';
            this.showAlert('danger', `<strong>Checkout Unsuccessful:</strong> ${escapeHtml(msg)}<br><small style="color: #991b1b;">Notice: Database transactions guaranteed no corrupt state. Items remain preserved in your cart.</small>`);
            
            // Re-enable button with a new idempotency key so user can retry or correct
            this.idempotencyKey = this.generateIdempotencyKey();
            if (btn) {
                btn.disabled = false;
                btn.textContent = `Try Again &bull; $${Number(this.cart.totalAmount).toFixed(2)}`;
            }
        }
    }

    showAlert(type, htmlContent) {
        const alertBox = document.getElementById('checkout-alert');
        if (!alertBox) return;

        const isSuccess = type === 'success';
        alertBox.style.display = 'block';
        alertBox.style.padding = '1rem 1.25rem';
        alertBox.style.borderRadius = 'var(--radius-md)';
        alertBox.style.fontSize = '0.9rem';
        alertBox.style.background = isSuccess ? '#ecfdf5' : '#fef2f2';
        alertBox.style.border = isSuccess ? '1px solid #6ee7b7' : '1px solid #fca5a5';
        alertBox.style.color = isSuccess ? '#065f46' : '#991b1b';
        alertBox.innerHTML = htmlContent;

        window.scrollTo({ top: 0, behavior: 'smooth' });
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

export const checkoutManager = new CheckoutManager();
document.addEventListener('DOMContentLoaded', () => {
    checkoutManager.init();
});
