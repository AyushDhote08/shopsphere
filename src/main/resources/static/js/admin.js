/**
 * ShopSphere Admin Console Module
 * Vanilla JavaScript ES6 module for Executive KPIs, Restocking Workflow,
 * Concurrency Inventory Adjustments, Order Fulfillment, and Real-Time Business Analytics.
 */
import { api } from './api.js';
import { auth } from './auth.js';

class AdminManager {
    constructor() {
        this.currentUser = null;
        this.activeTab = 'overview';

        // Overview date range (default last 30 days)
        this.overviewDays = 30;
        this.startDate = this.formatDate(new Date(Date.now() - 29 * 86400000));
        this.endDate = this.formatDate(new Date());

        // Inventory state
        this.invPage = 0;
        this.invQuery = '';
        this.invLowStockOnly = false;
        this.movementsPage = 0;

        // Orders state
        this.ordersPage = 0;
        this.orderStatusFilter = '';
        this.orderSearchQuery = '';
    }

    formatDate(d) {
        return d.toISOString().split('T')[0];
    }

    async init() {
        this.currentUser = await auth.getCurrentUser();
        const main = document.querySelector('main');

        if (!this.currentUser) {
            window.location.href = '/login.html?redirect=/admin.html';
            return;
        }

        if (this.currentUser.role !== 'ROLE_ADMIN') {
            if (main) {
                main.innerHTML = `
                    <div class="empty-state" style="margin-top: 3rem; background: white; border-color: #fca5a5;">
                        <div class="empty-state-icon" style="color: var(--danger);">&#128274;</div>
                        <h2 style="color: var(--danger); font-size: 1.5rem;">Access Denied — Administrator Privileges Required</h2>
                        <p style="max-width: 600px; margin: 0.75rem auto; color: var(--text-secondary);">
                            Your authenticated account (<code>${escapeHtml(this.currentUser.email)}</code>) has the role <strong>${escapeHtml(this.currentUser.role)}</strong>. 
                            Administrative dashboards, inventory restocking, and financial sales reports are strictly restricted to <strong>ROLE_ADMIN</strong>.
                        </p>
                        <div style="display: flex; gap: 1rem; justify-content: center; margin-top: 1.5rem;">
                            <a href="/index.html" class="btn btn-primary">&larr; Return to Storefront</a>
                            <button id="btn-switch-admin-login" class="btn btn-outline">Sign In with Admin Account</button>
                        </div>
                    </div>
                `;
                document.getElementById('btn-switch-admin-login')?.addEventListener('click', async () => {
                    await auth.logout();
                });
            }
            return;
        }

        // Show admin user tag
        const tag = document.getElementById('admin-user-tag');
        const nameEl = document.getElementById('admin-user-name');
        if (tag && nameEl) {
            nameEl.textContent = `${this.currentUser.fullName} (${this.currentUser.email})`;
            tag.style.display = 'inline-flex';
        }

        // Initialize date inputs
        const startInput = document.getElementById('overview-start-date');
        const endInput = document.getElementById('overview-end-date');
        if (startInput) startInput.value = this.startDate;
        if (endInput) endInput.value = this.endDate;

        this.bindEvents();
        await this.loadOverview();
    }

    bindEvents() {
        // Tab switching
        document.querySelectorAll('.admin-tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tab = e.currentTarget.dataset.tab;
                this.switchTab(tab);
            });
        });

        // Overview date preset buttons
        document.querySelectorAll('.range-preset-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.range-preset-btn').forEach(b => b.classList.remove('active'));
                e.currentTarget.classList.add('active');
                const days = parseInt(e.currentTarget.dataset.days, 10);
                this.overviewDays = days;
                this.endDate = this.formatDate(new Date());
                this.startDate = this.formatDate(new Date(Date.now() - (days - 1) * 86400000));

                const startInput = document.getElementById('overview-start-date');
                const endInput = document.getElementById('overview-end-date');
                if (startInput) startInput.value = this.startDate;
                if (endInput) endInput.value = this.endDate;

                this.loadOverview();
            });
        });

        // Overview apply custom date range
        document.getElementById('btn-apply-overview-date')?.addEventListener('click', () => {
            const startInput = document.getElementById('overview-start-date');
            const endInput = document.getElementById('overview-end-date');
            if (startInput && endInput) {
                if (startInput.value > endInput.value) {
                    this.showAlert('danger', 'Start date cannot be after end date.');
                    return;
                }
                document.querySelectorAll('.range-preset-btn').forEach(b => b.classList.remove('active'));
                this.startDate = startInput.value;
                this.endDate = endInput.value;
                this.loadOverview();
            }
        });

        // Inventory search & filter
        const invSearch = document.getElementById('inv-search');
        let searchTimer = null;
        invSearch?.addEventListener('input', (e) => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                this.invQuery = e.target.value.trim();
                this.invPage = 0;
                this.loadInventory();
            }, 350);
        });

        document.getElementById('inv-low-stock-filter')?.addEventListener('change', (e) => {
            this.invLowStockOnly = e.target.checked;
            this.invPage = 0;
            this.loadInventory();
        });

        // Toggle movements log view
        document.getElementById('btn-toggle-movements')?.addEventListener('click', () => {
            const sec = document.getElementById('movements-section');
            if (sec) {
                sec.style.display = 'block';
                sec.scrollIntoView({ behavior: 'smooth' });
                this.loadMovements();
            }
        });
        document.getElementById('btn-close-movements')?.addEventListener('click', () => {
            const sec = document.getElementById('movements-section');
            if (sec) sec.style.display = 'none';
        });

        // Orders filter
        document.getElementById('btn-filter-orders')?.addEventListener('click', () => {
            this.orderSearchQuery = document.getElementById('order-search-input')?.value.trim() || '';
            this.orderStatusFilter = document.getElementById('order-status-filter')?.value || '';
            this.ordersPage = 0;
            this.loadOrders();
        });

        // Modal close buttons
        document.querySelectorAll('.btn-close-modal').forEach(btn => {
            btn.addEventListener('click', () => this.closeAllModals());
        });
        document.querySelectorAll('.modal-backdrop').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) this.closeAllModals();
            });
        });

        // Restock modal submit
        document.getElementById('btn-submit-restock')?.addEventListener('click', () => this.submitRestock());

        // Stock adjustment modal submit
        document.getElementById('btn-submit-adjust')?.addEventListener('click', () => this.submitStockAdjustment());

        // Threshold modal submit
        document.getElementById('btn-submit-threshold')?.addEventListener('click', () => this.submitThreshold());
    }

    switchTab(tab) {
        this.activeTab = tab;
        document.querySelectorAll('.admin-tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tab);
        });
        document.querySelectorAll('.admin-tab-pane').forEach(pane => {
            pane.classList.toggle('active', pane.id === `tab-${tab}`);
        });

        if (tab === 'overview') this.loadOverview();
        else if (tab === 'inventory') this.loadInventory();
        else if (tab === 'orders') this.loadOrders();
        else if (tab === 'analytics') this.loadAnalytics();
    }

    // =========================================================================
    // TAB 1: OVERVIEW & KPIS
    // =========================================================================
    async loadOverview() {
        try {
            const [summaryRes, salesRes, topRes] = await Promise.all([
                api.get(`/api/admin/dashboard?startDate=${this.startDate}&endDate=${this.endDate}`),
                api.get(`/api/admin/dashboard/sales?startDate=${this.startDate}&endDate=${this.endDate}`),
                api.get(`/api/admin/dashboard/top-products?limit=5&startDate=${this.startDate}&endDate=${this.endDate}`)
            ]);

            const summary = summaryRes.data;
            const sales = salesRes.data;
            const topProducts = topRes.data;

            // Render KPI cards
            document.getElementById('kpi-revenue').textContent = `$${Number(summary.periodRevenue).toFixed(2)}`;
            document.getElementById('kpi-profit').textContent = `$${Number(summary.periodProfit).toFixed(2)}`;
            document.getElementById('kpi-orders-count').textContent = summary.periodOrdersCount;
            document.getElementById('kpi-aov').textContent = `Avg Order: $${Number(sales.averageOrderValue).toFixed(2)}`;
            document.getElementById('kpi-lowStock').textContent = summary.lowStockCount;
            document.getElementById('kpi-pending-orders').textContent = summary.ordersAwaitingProcessing;
            document.getElementById('kpi-customers').textContent = summary.totalCustomers;

            // Render Daily Sales Timeline Chart
            this.renderSalesChart(sales.dailySales);

            // Render Top Products
            this.renderTopProducts(topProducts);
        } catch (err) {
            console.error('Failed to load dashboard overview:', err);
            this.showAlert('danger', 'Failed to load executive dashboard: ' + (err.message || 'Server error'));
        }
    }

    renderSalesChart(dailySales) {
        const container = document.getElementById('sales-bars-container');
        if (!container) return;

        if (!dailySales || dailySales.length === 0) {
            container.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 2rem;">No sales records found for this period.</div>';
            return;
        }

        // Find max revenue for scaling bars
        const maxVal = Math.max(...dailySales.map(d => Number(d.revenue)), 100);

        const barsHtml = dailySales.map(d => {
            const rev = Number(d.revenue);
            const prof = Number(d.profit);
            const revHeight = Math.max(Math.round((rev / maxVal) * 100), 4);
            const profHeight = Math.max(Math.round((prof / maxVal) * 100), 2);
            const label = d.date.split('-').slice(1).join('/'); // MM/DD

            return `
                <div class="chart-bar-col" title="${d.date}: $${rev.toFixed(2)} Revenue ($${prof.toFixed(2)} Profit, ${d.orderCount} orders)">
                    <div class="chart-bar-stack">
                        <div class="chart-bar-rev" style="height: ${revHeight}%;"></div>
                        <div class="chart-bar-prof" style="height: ${profHeight}%;"></div>
                    </div>
                    <div class="chart-bar-date">${label}</div>
                </div>
            `;
        }).join('');

        container.innerHTML = barsHtml;
    }

    renderTopProducts(products) {
        const tbody = document.getElementById('top-products-tbody');
        if (!tbody) return;

        if (!products || products.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">No products sold in this period.</td></tr>';
            return;
        }

        tbody.innerHTML = products.map((p, idx) => `
            <tr>
                <td>
                    <div style="font-weight: 700; color: var(--text-primary);">${idx + 1}. ${escapeHtml(p.productName)}</div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); font-family: monospace;">SKU: ${escapeHtml(p.productSku)}</div>
                </td>
                <td><span class="badge-subtle">${escapeHtml(p.categoryName)}</span></td>
                <td style="text-align: center; font-weight: 700; color: var(--text-primary);">${p.unitsSold}</td>
                <td style="text-align: right; font-weight: 700; color: var(--primary);">$${Number(p.totalRevenue).toFixed(2)}</td>
                <td style="text-align: right; font-weight: 700; color: var(--success);">$${Number(p.totalProfit).toFixed(2)}</td>
            </tr>
        `).join('');
    }

    // =========================================================================
    // TAB 2: INVENTORY MANAGEMENT
    // =========================================================================
    async loadInventory() {
        const tbody = document.getElementById('inventory-tbody');
        if (!tbody) return;

        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading inventory records...</td></tr>';

        try {
            const params = new URLSearchParams({
                page: this.invPage,
                size: 10,
                lowStockOnly: this.invLowStockOnly
            });
            if (this.invQuery) params.append('query', this.invQuery);

            const res = await api.get(`/api/admin/inventory?${params.toString()}`);
            const pageData = res.data;
            const products = pageData.content || [];

            if (products.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">No matching products found.</td></tr>';
                document.getElementById('inventory-pagination').style.display = 'none';
                return;
            }

            tbody.innerHTML = products.map(p => {
                const isLow = p.stockQuantity <= p.lowStockThreshold;
                const stockBadge = isLow
                    ? `<span class="stock-pill stock-out">&#9888; Low: ${p.stockQuantity}</span>`
                    : `<span class="stock-pill stock-in">&#10003; ${p.stockQuantity}</span>`;

                return `
                    <tr data-product-id="${p.id}">
                        <td style="font-family: monospace; font-size: 0.8rem; font-weight: 600;">${escapeHtml(p.sku)}</td>
                        <td style="font-weight: 700; color: var(--text-primary);">
                            <a href="/product.html?id=${p.id}" target="_blank" style="color: inherit; text-decoration: none;">
                                ${escapeHtml(p.name)} &nearr;
                            </a>
                        </td>
                        <td><span class="badge-subtle">${escapeHtml(p.categoryName || 'General')}</span></td>
                        <td style="text-align: right; color: var(--text-muted); font-size: 0.9rem;">$${Number(p.costPrice).toFixed(2)}</td>
                        <td style="text-align: right; font-weight: 700; color: var(--text-primary); font-size: 0.9rem;">$${Number(p.sellingPrice).toFixed(2)}</td>
                        <td style="text-align: center;">${stockBadge}</td>
                        <td style="text-align: center; font-weight: 600; color: var(--text-secondary);">${p.lowStockThreshold}</td>
                        <td style="text-align: center;">
                            <div style="display: flex; gap: 0.35rem; justify-content: center;">
                                <button class="btn btn-sm btn-primary btn-open-restock" 
                                        data-product-id="${p.id}" 
                                        data-product-name="${escapeHtml(p.name)}" 
                                        data-product-sku="${escapeHtml(p.sku)}" 
                                        data-current-stock="${p.stockQuantity}">
                                    + Restock
                                </button>
                                <button class="btn btn-sm btn-outline btn-open-adjust" 
                                        data-product-id="${p.id}" 
                                        data-product-name="${escapeHtml(p.name)}" 
                                        data-product-sku="${escapeHtml(p.sku)}" 
                                        data-current-stock="${p.stockQuantity}">
                                    Adjust
                                </button>
                                <button class="btn btn-sm btn-outline btn-open-threshold" 
                                        data-product-id="${p.id}" 
                                        data-product-name="${escapeHtml(p.name)}" 
                                        data-product-sku="${escapeHtml(p.sku)}" 
                                        data-current-threshold="${p.lowStockThreshold}"
                                        title="Configure reorder alert threshold">
                                    &#9881;
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');

            this.bindInventoryRowActions();
            this.renderPagination('inventory-pagination', pageData, (p) => {
                this.invPage = p;
                this.loadInventory();
            });
        } catch (err) {
            console.error('Failed to load inventory:', err);
            tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--danger); padding: 2rem;">Error: ${escapeHtml(err.message)}</td></tr>`;
        }
    }

    bindInventoryRowActions() {
        // Open Restock Modal
        document.querySelectorAll('.btn-open-restock').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const b = e.currentTarget;
                document.getElementById('restock-product-id').value = b.dataset.productId;
                document.getElementById('restock-product-name').textContent = b.dataset.productName;
                document.getElementById('restock-product-sku').textContent = `SKU: ${b.dataset.productSku}`;
                document.getElementById('restock-current-stock').textContent = `Current Stock: ${b.dataset.currentStock}`;
                document.getElementById('restock-qty').value = '20';
                document.getElementById('restock-ref').value = '';
                document.getElementById('restock-reason').value = '';
                this.openModal('modal-restock');
            });
        });

        // Open Adjust Modal
        document.querySelectorAll('.btn-open-adjust').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const b = e.currentTarget;
                document.getElementById('adjust-product-id').value = b.dataset.productId;
                document.getElementById('adjust-product-name').textContent = b.dataset.productName;
                document.getElementById('adjust-product-sku').textContent = `SKU: ${b.dataset.productSku}`;
                document.getElementById('adjust-current-stock').textContent = `Current Stock: ${b.dataset.currentStock}`;
                document.getElementById('adjust-qty').value = '';
                document.getElementById('adjust-reason').value = '';
                document.getElementById('adjust-ref').value = '';
                this.openModal('modal-adjust');
            });
        });

        // Open Threshold Modal
        document.querySelectorAll('.btn-open-threshold').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const b = e.currentTarget;
                document.getElementById('threshold-product-id').value = b.dataset.productId;
                document.getElementById('threshold-product-name').textContent = b.dataset.productName;
                document.getElementById('threshold-product-sku').textContent = `SKU: ${b.dataset.productSku}`;
                document.getElementById('threshold-value').value = b.dataset.currentThreshold;
                this.openModal('modal-threshold');
            });
        });
    }

    async submitRestock() {
        const productId = parseInt(document.getElementById('restock-product-id').value, 10);
        const quantity = parseInt(document.getElementById('restock-qty').value, 10);
        const referenceId = document.getElementById('restock-ref').value.trim();
        const reason = document.getElementById('restock-reason').value.trim();

        if (!quantity || quantity <= 0) {
            alert('Please enter a valid positive restock quantity.');
            return;
        }

        const btn = document.getElementById('btn-submit-restock');
        if (btn) btn.disabled = true;

        try {
            await api.post('/api/admin/inventory/restock', {
                productId,
                quantity,
                referenceId,
                reason
            });
            this.closeAllModals();
            this.showAlert('success', 'Inventory successfully restocked with transactional audit record.');
            await this.loadInventory();
        } catch (err) {
            alert('Restock failed: ' + (err.message || 'Server error'));
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    async submitStockAdjustment() {
        const productId = parseInt(document.getElementById('adjust-product-id').value, 10);
        const quantityAdjustment = parseInt(document.getElementById('adjust-qty').value, 10);
        const reason = document.getElementById('adjust-reason').value.trim();
        const referenceId = document.getElementById('adjust-ref').value.trim();

        if (isNaN(quantityAdjustment) || quantityAdjustment === 0) {
            alert('Please enter a non-zero quantity adjustment.');
            return;
        }
        if (!reason || reason.length < 3) {
            alert('A mandatory justification reason of at least 3 characters is required for manual stock adjustments.');
            return;
        }

        const btn = document.getElementById('btn-submit-adjust');
        if (btn) btn.disabled = true;

        try {
            await api.post('/api/admin/inventory/adjustments', {
                productId,
                quantityAdjustment,
                reason,
                referenceId
            });
            this.closeAllModals();
            this.showAlert('success', 'Stock adjustment successfully applied and logged.');
            await this.loadInventory();
        } catch (err) {
            alert('Adjustment rejected: ' + (err.message || 'Server error'));
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    async submitThreshold() {
        const productId = parseInt(document.getElementById('threshold-product-id').value, 10);
        const threshold = parseInt(document.getElementById('threshold-value').value, 10);

        if (isNaN(threshold) || threshold < 0) {
            alert('Low stock threshold must be a non-negative number.');
            return;
        }

        const btn = document.getElementById('btn-submit-threshold');
        if (btn) btn.disabled = true;

        try {
            await api.patch(`/api/admin/inventory/products/${productId}/threshold`, {
                lowStockThreshold: threshold
            });
            this.closeAllModals();
            this.showAlert('success', 'Reorder threshold successfully updated.');
            await this.loadInventory();
        } catch (err) {
            alert('Failed to update threshold: ' + (err.message || 'Server error'));
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    async loadMovements() {
        const tbody = document.getElementById('movements-tbody');
        if (!tbody) return;

        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading movements log...</td></tr>';

        try {
            const res = await api.get(`/api/admin/inventory/movements?page=${this.movementsPage}&size=10`);
            const pageData = res.data;
            const movements = pageData.content || [];

            if (movements.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">No inventory movements recorded yet.</td></tr>';
                return;
            }

            tbody.innerHTML = movements.map(m => {
                const dateStr = new Date(m.createdAt).toLocaleString('en-US', {
                    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                });
                const delta = m.quantityChanged > 0 ? `+${m.quantityChanged}` : `${m.quantityChanged}`;
                const deltaColor = m.quantityChanged > 0 ? 'var(--success)' : 'var(--danger)';

                let badgeClass = 'status-pending';
                if (m.movementType === 'RESTOCK') badgeClass = 'status-paid';
                else if (m.movementType === 'ORDER_DEDUCTION') badgeClass = 'status-shipped';
                else if (m.movementType === 'ORDER_CANCELLED_RESTORE') badgeClass = 'status-confirmed';

                return `
                    <tr>
                        <td style="font-size: 0.8rem; color: var(--text-secondary); white-space: nowrap;">${dateStr}</td>
                        <td>
                            <div style="font-weight: 700; color: var(--text-primary); font-size: 0.85rem;">${escapeHtml(m.productName || 'Product')}</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted); font-family: monospace;">SKU: ${escapeHtml(m.productSku || 'N/A')}</div>
                        </td>
                        <td style="text-align: center;">
                            <span class="status-badge ${badgeClass}" style="font-size: 0.7rem;">${escapeHtml(m.movementType)}</span>
                        </td>
                        <td style="text-align: center; font-weight: 600; color: var(--text-secondary);">${m.stockBefore != null ? m.stockBefore : '-'}</td>
                        <td style="text-align: center; font-weight: 800; color: ${deltaColor};">${delta}</td>
                        <td style="text-align: center; font-weight: 700; color: var(--text-primary);">${m.stockAfter}</td>
                        <td style="font-size: 0.85rem; max-width: 220px; line-height: 1.3;">
                            <div style="color: var(--text-primary); font-weight: 500;">${escapeHtml(m.reason || '-')}</div>
                            ${m.referenceId ? `<div style="font-size: 0.75rem; color: var(--text-muted); font-family: monospace;">Ref: ${escapeHtml(m.referenceId)}</div>` : ''}
                        </td>
                        <td style="font-size: 0.8rem; color: var(--text-secondary);">${escapeHtml(m.performedBy || 'System')}</td>
                    </tr>
                `;
            }).join('');

            this.renderPagination('movements-pagination', pageData, (p) => {
                this.movementsPage = p;
                this.loadMovements();
            });
        } catch (err) {
            console.error('Failed to load movements:', err);
            tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--danger); padding: 2rem;">Error: ${escapeHtml(err.message)}</td></tr>`;
        }
    }

    // =========================================================================
    // TAB 3: ORDER FULFILLMENT MANAGEMENT
    // =========================================================================
    async loadOrders() {
        const tbody = document.getElementById('admin-orders-tbody');
        if (!tbody) return;

        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading customer orders...</td></tr>';

        try {
            const params = new URLSearchParams({
                page: this.ordersPage,
                size: 10
            });
            if (this.orderStatusFilter) params.append('status', this.orderStatusFilter);
            if (this.orderSearchQuery) params.append('orderNumber', this.orderSearchQuery);

            const res = await api.get(`/api/admin/orders?${params.toString()}`);
            const pageData = res.data;
            const orders = pageData.content || [];

            if (orders.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">No matching customer orders found.</td></tr>';
                document.getElementById('admin-orders-pagination').style.display = 'none';
                return;
            }

            tbody.innerHTML = orders.map(o => {
                const dateStr = new Date(o.createdAt).toLocaleDateString('en-US', {
                    month: 'short', day: 'numeric', year: 'numeric'
                });

                return `
                    <tr>
                        <td style="font-family: monospace; font-weight: 700; color: var(--text-primary); font-size: 0.85rem;">
                            ${escapeHtml(o.orderNumber)}
                        </td>
                        <td style="font-size: 0.85rem; color: var(--text-secondary); white-space: nowrap;">${dateStr}</td>
                        <td>
                            <div style="font-weight: 600; color: var(--text-primary); font-size: 0.85rem;">${escapeHtml(o.customerName || 'Customer')}</div>
                            <div style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(o.customerEmail || '')}</div>
                        </td>
                        <td style="text-align: right; font-weight: 800; color: var(--primary);">$${Number(o.totalAmount).toFixed(2)}</td>
                        <td style="text-align: right; font-weight: 700; color: var(--success);">$${Number(o.grossProfit).toFixed(2)}</td>
                        <td style="text-align: center;">
                            <span class="status-badge status-${o.orderStatus ? o.orderStatus.toLowerCase() : 'pending'}">
                                ${escapeHtml(o.orderStatus)}
                            </span>
                        </td>
                        <td style="text-align: center;">
                            <span class="status-badge status-${o.paymentStatus ? o.paymentStatus.toLowerCase() : 'paid'}">
                                ${escapeHtml(o.paymentStatus)}
                            </span>
                        </td>
                        <td style="text-align: center;">
                            <button class="btn btn-primary btn-sm btn-manage-order" data-order-id="${o.id}">
                                Manage &rarr;
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');

            // Bind manage click
            document.querySelectorAll('.btn-manage-order').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const orderId = parseInt(e.currentTarget.dataset.orderId, 10);
                    this.openOrderManageModal(orderId);
                });
            });

            this.renderPagination('admin-orders-pagination', pageData, (p) => {
                this.ordersPage = p;
                this.loadOrders();
            });
        } catch (err) {
            console.error('Failed to load orders:', err);
            tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--danger); padding: 2rem;">Error: ${escapeHtml(err.message)}</td></tr>`;
        }
    }

    async openOrderManageModal(orderId) {
        try {
            const res = await api.get(`/api/admin/orders/${orderId}`);
            const o = res.data;

            const modalTitle = document.getElementById('modal-order-number');
            const modalBody = document.getElementById('modal-order-body');
            const actionsEl = document.getElementById('order-status-actions');

            if (modalTitle) modalTitle.textContent = `Order ${o.orderNumber}`;

            const addr = o.shippingAddress;
            const addrText = addr ? `
                <strong>${escapeHtml(addr.recipientName)}</strong> (Phone: ${escapeHtml(addr.phone)})<br>
                ${escapeHtml(addr.streetAddress)}, ${escapeHtml(addr.city)}, ${escapeHtml(addr.state)} ${escapeHtml(addr.postalCode)} &bull; ${escapeHtml(addr.country)}
            ` : 'Shipping destination unavailable';

            const itemsRows = (o.items || []).map(it => `
                <tr>
                    <td style="padding: 0.6rem 0.5rem; border-bottom: 1px solid var(--border-color);">
                        <div style="font-weight: 700; color: var(--text-primary); font-size: 0.85rem;">${escapeHtml(it.productName)}</div>
                        <div style="font-size: 0.75rem; color: var(--text-muted); font-family: monospace;">SKU: ${escapeHtml(it.productSku)}</div>
                    </td>
                    <td style="padding: 0.6rem 0.5rem; border-bottom: 1px solid var(--border-color); text-align: right; color: var(--text-secondary);">$${Number(it.unitPrice).toFixed(2)}</td>
                    <td style="padding: 0.6rem 0.5rem; border-bottom: 1px solid var(--border-color); text-align: right; color: var(--text-muted);">$${Number(it.unitCost).toFixed(2)}</td>
                    <td style="padding: 0.6rem 0.5rem; border-bottom: 1px solid var(--border-color); text-align: center; font-weight: 600;">${it.quantity}</td>
                    <td style="padding: 0.6rem 0.5rem; border-bottom: 1px solid var(--border-color); text-align: right; font-weight: 700;">$${Number(it.lineTotal).toFixed(2)}</td>
                    <td style="padding: 0.6rem 0.5rem; border-bottom: 1px solid var(--border-color); text-align: right; font-weight: 700; color: var(--success);">$${Number(it.grossProfit).toFixed(2)}</td>
                </tr>
            `).join('');

            if (modalBody) {
                modalBody.innerHTML = `
                    <!-- Meta Summary Banner -->
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 1rem; background: #f8fafc; border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 1.25rem;">
                        <div>
                            <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Status</div>
                            <div style="margin-top: 0.25rem;"><span class="status-badge status-${o.orderStatus ? o.orderStatus.toLowerCase() : 'pending'}">${escapeHtml(o.orderStatus)}</span></div>
                        </div>
                        <div>
                            <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Payment Status</div>
                            <div style="margin-top: 0.25rem;"><span class="status-badge status-${o.paymentStatus ? o.paymentStatus.toLowerCase() : 'paid'}">${escapeHtml(o.paymentStatus)}</span></div>
                        </div>
                        <div>
                            <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Payment Method</div>
                            <div style="font-weight: 600; font-size: 0.85rem; color: var(--text-primary); margin-top: 0.25rem;">${escapeHtml(o.paymentMethod || 'SIMULATED')}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600;">Order Total</div>
                            <div style="font-weight: 800; font-size: 1rem; color: var(--primary); margin-top: 0.25rem;">$${Number(o.totalAmount).toFixed(2)}</div>
                        </div>
                    </div>

                    <!-- Customer & Shipping -->
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.25rem;">
                        <div style="background: white; border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 0.85rem 1rem; font-size: 0.85rem;">
                            <div style="font-size: 0.75rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; margin-bottom: 0.35rem;">Customer Account</div>
                            <strong>${escapeHtml(o.customerName || 'N/A')}</strong><br>
                            Email: ${escapeHtml(o.customerEmail || 'N/A')}<br>
                            Phone: ${escapeHtml(o.customerPhone || 'N/A')}
                        </div>
                        <div style="background: white; border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 0.85rem 1rem; font-size: 0.85rem; line-height: 1.4;">
                            <div style="font-size: 0.75rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; margin-bottom: 0.35rem;">Delivery Destination</div>
                            ${addrText}
                        </div>
                    </div>

                    ${o.cancellationReason ? `
                        <div style="background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; padding: 0.75rem 1rem; border-radius: var(--radius-sm); font-size: 0.85rem; margin-bottom: 1.25rem;">
                            <strong>Cancellation Reason:</strong> ${escapeHtml(o.cancellationReason)}
                        </div>
                    ` : ''}

                    <!-- Items snapshot table -->
                    <div style="margin-bottom: 1.25rem;">
                        <div style="font-size: 0.8rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.5rem;">Purchased Items & Profit Margin Snapshot</div>
                        <div style="overflow-x: auto;">
                            <table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">
                                <thead>
                                    <tr style="background: #f8fafc; border-bottom: 1px solid var(--border-color); text-align: left; font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">
                                        <th style="padding: 0.5rem;">Item Description</th>
                                        <th style="padding: 0.5rem; text-align: right;">Unit Price</th>
                                        <th style="padding: 0.5rem; text-align: right;">Unit Cost</th>
                                        <th style="padding: 0.5rem; text-align: center;">Qty</th>
                                        <th style="padding: 0.5rem; text-align: right;">Line Total</th>
                                        <th style="padding: 0.5rem; text-align: right;">Gross Profit</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${itemsRows}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- Financial breakdown -->
                    <div style="max-width: 280px; margin-left: auto; font-size: 0.85rem; background: #f8fafc; padding: 0.85rem; border-radius: var(--radius-sm);">
                        <div class="summary-line"><span>Subtotal:</span> <span>$${Number(o.subtotalAmount).toFixed(2)}</span></div>
                        <div class="summary-line"><span>Tax (5%):</span> <span>$${Number(o.taxAmount).toFixed(2)}</span></div>
                        <div class="summary-line"><span>Shipping:</span> <span>$${Number(o.shippingFee).toFixed(2)}</span></div>
                        <div class="summary-line" style="border-top: 1px solid var(--border-color); padding-top: 0.4rem; font-weight: 700;"><span>Total:</span> <span>$${Number(o.totalAmount).toFixed(2)}</span></div>
                        <div class="summary-line" style="color: var(--success); font-weight: 700;"><span>Gross Profit:</span> <span>$${Number(o.grossProfit).toFixed(2)}</span></div>
                    </div>
                `;
            }

            // Build transition action buttons
            if (actionsEl) {
                let btnsHtml = '';
                if (o.orderStatus === 'PENDING') {
                    btnsHtml = `
                        <button class="btn btn-sm btn-primary btn-transition-status" data-target-status="CONFIRMED">Confirm Order</button>
                        <button class="btn btn-sm btn-outline btn-transition-status" data-target-status="CANCELLED" style="color: var(--danger); border-color: #fca5a5;">Cancel Order</button>
                    `;
                } else if (o.orderStatus === 'CONFIRMED') {
                    btnsHtml = `
                        <button class="btn btn-sm btn-primary btn-transition-status" data-target-status="SHIPPED">Mark as SHIPPED</button>
                        <button class="btn btn-sm btn-outline btn-transition-status" data-target-status="CANCELLED" style="color: var(--danger); border-color: #fca5a5;">Cancel Order</button>
                    `;
                } else if (o.orderStatus === 'SHIPPED') {
                    btnsHtml = `
                        <button class="btn btn-sm btn-primary btn-transition-status" data-target-status="DELIVERED">Mark as DELIVERED</button>
                    `;
                } else {
                    btnsHtml = `<span style="font-size: 0.8rem; color: var(--text-muted);">&#128274; Terminal State (${o.orderStatus})</span>`;
                }
                actionsEl.innerHTML = btnsHtml;

                // Bind transitions
                actionsEl.querySelectorAll('.btn-transition-status').forEach(b => {
                    b.addEventListener('click', async (e) => {
                        const targetStatus = e.currentTarget.dataset.targetStatus;
                        await this.executeStatusTransition(o.id, targetStatus);
                    });
                });
            }

            this.openModal('modal-order-manage');
        } catch (err) {
            alert('Failed to load order details: ' + (err.message || 'Server error'));
        }
    }

    async executeStatusTransition(orderId, newStatus) {
        let reason = '';
        if (newStatus === 'CANCELLED') {
            reason = prompt('Please provide a reason for cancelling this order (stock will be automatically restored):', 'Cancelled by Admin');
            if (reason === null) return; // user cancelled prompt
        }

        try {
            await api.patch(`/api/admin/orders/${orderId}/status`, {
                newStatus,
                reason
            });
            this.showAlert('success', `Order status successfully transitioned to ${newStatus}.`);
            await this.openOrderManageModal(orderId);
            await this.loadOrders();
        } catch (err) {
            alert('Status transition rejected: ' + (err.message || 'Server error'));
        }
    }

    // =========================================================================
    // TAB 4: REPORTS & VALUATION
    // =========================================================================
    async loadAnalytics() {
        try {
            const [valRes, catRes] = await Promise.all([
                api.get('/api/admin/reports/inventory'),
                api.get(`/api/admin/reports/categories?startDate=${this.startDate}&endDate=${this.endDate}`)
            ]);

            const val = valRes.data;
            const categories = catRes.data;

            // Render valuation
            document.getElementById('val-cost').textContent = `$${Number(val.totalCostValue).toFixed(2)}`;
            document.getElementById('val-retail').textContent = `$${Number(val.totalRetailValue).toFixed(2)}`;
            document.getElementById('val-profit').textContent = `$${Number(val.potentialGrossProfit).toFixed(2)}`;
            document.getElementById('val-units').textContent = val.totalUnitsInStock.toLocaleString();
            document.getElementById('val-prods').textContent = `Across ${val.totalProductsCount} products (${val.lowStockProductsCount} low-stock)`;

            // Render Category Sales
            const catTbody = document.getElementById('category-sales-tbody');
            if (catTbody) {
                if (!categories || categories.length === 0) {
                    catTbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">No category sales recorded in this period.</td></tr>';
                } else {
                    catTbody.innerHTML = categories.map(c => {
                        const rev = Number(c.revenue);
                        const prof = Number(c.profit);
                        const margin = rev > 0 ? ((prof / rev) * 100).toFixed(1) : '0.0';

                        return `
                            <tr>
                                <td style="font-weight: 700; color: var(--text-primary);">${escapeHtml(c.categoryName)}</td>
                                <td style="text-align: center; font-weight: 600;">${c.unitsSold}</td>
                                <td style="text-align: right; font-weight: 700; color: var(--primary);">$${rev.toFixed(2)}</td>
                                <td style="text-align: right; font-weight: 700; color: var(--success);">$${prof.toFixed(2)}</td>
                                <td style="text-align: right; font-weight: 600; color: var(--text-secondary);">${margin}%</td>
                            </tr>
                        `;
                    }).join('');
                }
            }
        } catch (err) {
            console.error('Failed to load reports:', err);
            this.showAlert('danger', 'Failed to load valuation reports: ' + (err.message || 'Server error'));
        }
    }

    // =========================================================================
    // MODAL HELPERS & UTILS
    // =========================================================================
    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'flex';
            setTimeout(() => modal.classList.add('open'), 10);
        }
    }

    closeAllModals() {
        document.querySelectorAll('.modal-backdrop').forEach(modal => {
            modal.classList.remove('open');
            setTimeout(() => { modal.style.display = 'none'; }, 200);
        });
    }

    renderPagination(containerId, pageData, onPageClick) {
        const container = document.getElementById(containerId);
        if (!container) return;

        const totalPages = pageData.totalPages || 1;
        const currentPage = pageData.pageNumber || 0;

        if (totalPages <= 1) {
            container.style.display = 'none';
            return;
        }

        container.style.display = 'flex';
        let html = `
            <button class="page-btn ${currentPage === 0 ? 'disabled' : ''}" 
                    data-page="${currentPage - 1}" 
                    ${currentPage === 0 ? 'disabled' : ''}>
                &larr; Prev
            </button>
        `;

        for (let i = 0; i < totalPages; i++) {
            html += `
                <button class="page-btn ${i === currentPage ? 'active' : ''}" 
                        data-page="${i}">
                    ${i + 1}
                </button>
            `;
        }

        html += `
            <button class="page-btn ${currentPage >= totalPages - 1 ? 'disabled' : ''}" 
                    data-page="${currentPage + 1}" 
                    ${currentPage >= totalPages - 1 ? 'disabled' : ''}>
                Next &rarr;
            </button>
        `;

        container.innerHTML = html;
        container.querySelectorAll('.page-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const p = parseInt(e.currentTarget.dataset.page, 10);
                if (!isNaN(p) && p >= 0 && p < totalPages && p !== currentPage) {
                    onPageClick(p);
                }
            });
        });
    }

    showAlert(type, msg) {
        const alertBox = document.getElementById('admin-alert');
        if (!alertBox) return;

        const isSuccess = type === 'success';
        alertBox.style.display = 'block';
        alertBox.style.padding = '0.85rem 1.25rem';
        alertBox.style.borderRadius = 'var(--radius-md)';
        alertBox.style.fontSize = '0.9rem';
        alertBox.style.background = isSuccess ? '#ecfdf5' : '#fef2f2';
        alertBox.style.border = isSuccess ? '1px solid #6ee7b7' : '1px solid #fca5a5';
        alertBox.style.color = isSuccess ? '#065f46' : '#991b1b';
        alertBox.innerHTML = msg;

        setTimeout(() => {
            alertBox.style.display = 'none';
        }, 6000);
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

export const adminManager = new AdminManager();
document.addEventListener('DOMContentLoaded', () => {
    adminManager.init();
});
