/**
 * ShopSphere Catalog Module
 * Vanilla JavaScript ES6 module for storefront catalog browsing,
 * search-as-you-type, multi-facet filtering, sorting, pagination, and quick-view modal.
 */
import { api } from './api.js';

// SVG placeholder fallback for broken or missing product images
const PLACEHOLDER_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='300' viewBox='0 0 400 300'%3E%3Crect width='400' height='300' fill='%23f1f5f9'/%3E%3Cpath d='M160 180l30-40 25 30 35-45 40 55H110z' fill='%23cbd5e1'/%3E%3Ccircle cx='150' cy='110' r='20' fill='%23cbd5e1'/%3E%3Ctext x='50%25' y='80%25' text-anchor='middle' fill='%2394a3b8' font-family='system-ui, sans-serif' font-size='14'%3ENo Image Available%3C/text%3E%3C/svg%3E";

class CatalogManager {
    constructor() {
        this.state = {
            query: '',
            categoryId: null,
            minPrice: null,
            maxPrice: null,
            inStockOnly: false,
            page: 0,
            size: 12,
            sortBy: 'createdAt',
            sortDirection: 'DESC'
        };

        this.categories = [];
        this.searchDebounceTimer = null;
        this.currentProductPage = null;
    }

    async init() {
        this.parseUrlParams();
        this.bindEvents();
        await this.loadCategories();
        await this.loadProducts();
    }

    parseUrlParams() {
        const params = new URLSearchParams(window.location.search);
        if (params.has('query')) this.state.query = params.get('query');
        if (params.has('categoryId')) this.state.categoryId = Number(params.get('categoryId'));
        if (params.has('minPrice')) this.state.minPrice = Number(params.get('minPrice'));
        if (params.has('maxPrice')) this.state.maxPrice = Number(params.get('maxPrice'));
        if (params.has('inStockOnly')) this.state.inStockOnly = params.get('inStockOnly') === 'true';
        if (params.has('page')) this.state.page = Math.max(0, parseInt(params.get('page'), 10));
        if (params.has('size')) this.state.size = parseInt(params.get('size'), 10) || 12;
        if (params.has('sortBy')) this.state.sortBy = params.get('sortBy');
        if (params.has('sortDirection')) this.state.sortDirection = params.get('sortDirection');

        // Sync form controls with initial URL state
        const searchInput = document.getElementById('search-query');
        if (searchInput && this.state.query) {
            searchInput.value = this.state.query;
            this.toggleClearButton(true);
        }

        const minPriceInput = document.getElementById('filter-min-price');
        if (minPriceInput && this.state.minPrice) minPriceInput.value = this.state.minPrice;

        const maxPriceInput = document.getElementById('filter-max-price');
        if (maxPriceInput && this.state.maxPrice) maxPriceInput.value = this.state.maxPrice;

        const inStockCheckbox = document.getElementById('filter-instock');
        if (inStockCheckbox) inStockCheckbox.checked = this.state.inStockOnly;

        const sortSelect = document.getElementById('sort-by');
        if (sortSelect) {
            const combinedSort = `${this.state.sortBy}:${this.state.sortDirection.toLowerCase()}`;
            sortSelect.value = combinedSort;
        }
    }

    updateUrlParams() {
        const params = new URLSearchParams();
        if (this.state.query) params.set('query', this.state.query);
        if (this.state.categoryId) params.set('categoryId', this.state.categoryId);
        if (this.state.minPrice) params.set('minPrice', this.state.minPrice);
        if (this.state.maxPrice) params.set('maxPrice', this.state.maxPrice);
        if (this.state.inStockOnly) params.set('inStockOnly', 'true');
        if (this.state.page > 0) params.set('page', this.state.page);
        if (this.state.sortBy !== 'createdAt') params.set('sortBy', this.state.sortBy);
        if (this.state.sortDirection !== 'DESC') params.set('sortDirection', this.state.sortDirection);

        const newUrl = window.location.pathname + (params.toString() ? `?${params.toString()}` : '');
        window.history.replaceState({}, '', newUrl);
    }

    bindEvents() {
        // Search input with debounce
        const searchInput = document.getElementById('search-query');
        const clearBtn = document.getElementById('search-clear');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const value = e.target.value.trim();
                this.toggleClearButton(value.length > 0);
                clearTimeout(this.searchDebounceTimer);
                this.searchDebounceTimer = setTimeout(() => {
                    this.state.query = value;
                    this.state.page = 0;
                    this.loadProducts();
                }, 350);
            });

            if (clearBtn) {
                clearBtn.addEventListener('click', () => {
                    searchInput.value = '';
                    this.toggleClearButton(false);
                    this.state.query = '';
                    this.state.page = 0;
                    this.loadProducts();
                });
            }
        }

        // Price range form
        const priceForm = document.getElementById('price-filter-form');
        if (priceForm) {
            priceForm.addEventListener('submit', (e) => {
                e.preventDefault();
                const minVal = document.getElementById('filter-min-price').value;
                const maxVal = document.getElementById('filter-max-price').value;

                this.state.minPrice = minVal ? Number(minVal) : null;
                this.state.maxPrice = maxVal ? Number(maxVal) : null;
                this.state.page = 0;
                this.loadProducts();
            });

            const resetPriceBtn = document.getElementById('btn-reset-price');
            if (resetPriceBtn) {
                resetPriceBtn.addEventListener('click', () => {
                    document.getElementById('filter-min-price').value = '';
                    document.getElementById('filter-max-price').value = '';
                    this.state.minPrice = null;
                    this.state.maxPrice = null;
                    this.state.page = 0;
                    this.loadProducts();
                });
            }
        }

        // In-stock only toggle
        const inStockCheckbox = document.getElementById('filter-instock');
        if (inStockCheckbox) {
            inStockCheckbox.addEventListener('change', (e) => {
                this.state.inStockOnly = e.target.checked;
                this.state.page = 0;
                this.loadProducts();
            });
        }

        // Sort selector
        const sortSelect = document.getElementById('sort-by');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                const [sortBy, dir] = e.target.value.split(':');
                this.state.sortBy = sortBy;
                this.state.sortDirection = (dir || 'asc').toUpperCase();
                this.state.page = 0;
                this.loadProducts();
            });
        }

        // Quick View Modal Backdrop Close & Keyboard ESC
        const modal = document.getElementById('quickview-modal');
        const modalClose = document.getElementById('modal-close');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) this.closeQuickView();
            });
            if (modalClose) {
                modalClose.addEventListener('click', () => this.closeQuickView());
            }
            window.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && modal.classList.contains('open')) {
                    this.closeQuickView();
                }
            });
        }
    }

    toggleClearButton(show) {
        const clearBtn = document.getElementById('search-clear');
        if (clearBtn) {
            clearBtn.style.display = show ? 'block' : 'none';
        }
    }

    async loadCategories() {
        try {
            const res = await api.get('/api/categories');
            this.categories = res.data || [];
            this.renderCategoryBar();
        } catch (err) {
            console.error('Failed to load categories:', err);
        }
    }

    renderCategoryBar() {
        const container = document.getElementById('category-bar');
        if (!container) return;

        let html = `
            <button class="category-pill ${this.state.categoryId === null ? 'active' : ''}" 
                    data-category-id="">All Categories</button>
        `;

        this.categories.forEach(cat => {
            const isActive = this.state.categoryId === cat.id;
            html += `
                <button class="category-pill ${isActive ? 'active' : ''}" 
                        data-category-id="${cat.id}">
                    ${escapeHtml(cat.name)}
                </button>
            `;
        });

        container.innerHTML = html;

        // Attach category pill click listeners
        container.querySelectorAll('.category-pill').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const catId = e.currentTarget.dataset.categoryId;
                this.state.categoryId = catId ? Number(catId) : null;
                this.state.page = 0;

                // Update active styles
                container.querySelectorAll('.category-pill').forEach(p => p.classList.remove('active'));
                e.currentTarget.classList.add('active');

                this.loadProducts();
            });
        });
    }

    async loadProducts() {
        const grid = document.getElementById('products-grid');
        const paginationContainer = document.getElementById('pagination-controls');
        this.renderSkeletons();
        this.renderActiveFilterChips();
        this.updateUrlParams();

        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            sortBy: this.state.sortBy,
            sortDirection: this.state.sortDirection
        });

        if (this.state.query) params.append('query', this.state.query);
        if (this.state.categoryId) params.append('categoryId', this.state.categoryId);
        if (this.state.minPrice !== null) params.append('minPrice', this.state.minPrice);
        if (this.state.maxPrice !== null) params.append('maxPrice', this.state.maxPrice);
        if (this.state.inStockOnly) params.append('inStockOnly', 'true');

        try {
            const response = await api.get(`/api/products?${params.toString()}`);
            const pageData = response.data;
            this.currentProductPage = pageData;

            this.renderProducts(pageData.content);
            this.renderPagination(pageData);
            this.updateCatalogMeta(pageData);
        } catch (err) {
            console.error('Error fetching products:', err);
            if (grid) {
                grid.innerHTML = `
                    <div class="empty-state" style="border-color: #fca5a5; background: #fff5f5;">
                        <div class="empty-state-icon" style="color: var(--danger);">&#9888;</div>
                        <h3>Failed to load products</h3>
                        <p>${escapeHtml(err.message || 'There was a problem reaching the product catalog server.')}</p>
                        <button id="btn-retry-catalog" class="btn btn-primary btn-sm">Retry Request</button>
                    </div>
                `;
                document.getElementById('btn-retry-catalog')?.addEventListener('click', () => this.loadProducts());
            }
            if (paginationContainer) paginationContainer.innerHTML = '';
        }
    }

    renderSkeletons() {
        const grid = document.getElementById('products-grid');
        if (!grid) return;

        let skeletonsHtml = '';
        for (let i = 0; i < 6; i++) {
            skeletonsHtml += `
                <div class="product-card skeleton skeleton-card"></div>
            `;
        }
        grid.innerHTML = skeletonsHtml;
    }

    renderProducts(products) {
        const grid = document.getElementById('products-grid');
        if (!grid) return;

        if (!products || products.length === 0) {
            grid.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">&#128269;</div>
                    <h3>No products found</h3>
                    <p>We couldn't find any products matching your search criteria. Try adjusting keywords, changing categories, or resetting filters.</p>
                    <button id="btn-empty-reset" class="btn btn-outline btn-sm">Clear All Filters</button>
                </div>
            `;
            document.getElementById('btn-empty-reset')?.addEventListener('click', () => this.resetAllFilters());
            return;
        }

        grid.innerHTML = products.map(product => {
            const price = Number(product.sellingPrice).toFixed(2);
            let stockBadge = '';
            if (!product.inStock) {
                stockBadge = '<span class="stock-pill out-of-stock">Out of Stock</span>';
            } else if (product.lowStock) {
                stockBadge = `<span class="stock-pill low-stock">Low Stock: ${product.stockQuantity} left</span>`;
            } else {
                stockBadge = `<span class="stock-pill in-stock">In Stock: ${product.stockQuantity}</span>`;
            }

            const imgUrl = product.imageUrl || PLACEHOLDER_IMAGE;

            return `
                <article class="product-card" data-product-id="${product.id}">
                    <div class="card-image-wrapper">
                        <span class="card-badge-top">${escapeHtml(product.categoryName || 'General')}</span>
                        <img src="${imgUrl}" 
                             alt="${escapeHtml(product.name)}" 
                             class="product-image" 
                             loading="lazy"
                             onerror="this.onerror=null; this.src='${PLACEHOLDER_IMAGE}'">
                    </div>
                    <div class="product-body">
                        <span class="product-sku">${escapeHtml(product.sku)}</span>
                        <h3 class="product-title" title="${escapeHtml(product.name)}">${escapeHtml(product.name)}</h3>
                        <p class="product-desc">${escapeHtml(product.description || 'Quality item available in our catalog.')}</p>
                        <div class="card-footer">
                            <span class="product-price">$${price}</span>
                            ${stockBadge}
                        </div>
                        <div class="card-actions">
                            <button class="btn btn-outline btn-sm btn-quick-view" data-product-id="${product.id}">Quick View</button>
                            <a href="/product.html?id=${product.id}" class="btn btn-primary btn-sm">Details</a>
                        </div>
                    </div>
                </article>
            `;
        }).join('');

        // Bind quick view buttons
        grid.querySelectorAll('.btn-quick-view').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = Number(e.currentTarget.dataset.productId);
                const product = products.find(p => p.id === id);
                if (product) this.openQuickView(product);
            });
        });
    }

    renderActiveFilterChips() {
        const container = document.getElementById('active-filters-bar');
        if (!container) return;

        const chips = [];

        if (this.state.query) {
            chips.push({
                label: `Search: "${this.state.query}"`,
                remove: () => {
                    this.state.query = '';
                    const input = document.getElementById('search-query');
                    if (input) input.value = '';
                    this.toggleClearButton(false);
                }
            });
        }

        if (this.state.categoryId) {
            const cat = this.categories.find(c => c.id === this.state.categoryId);
            const catName = cat ? cat.name : `Category #${this.state.categoryId}`;
            chips.push({
                label: `Category: ${catName}`,
                remove: () => {
                    this.state.categoryId = null;
                    document.querySelectorAll('.category-pill').forEach(p => p.classList.remove('active'));
                    document.querySelector('.category-pill[data-category-id=""]')?.classList.add('active');
                }
            });
        }

        if (this.state.minPrice !== null) {
            chips.push({
                label: `Min: $${this.state.minPrice}`,
                remove: () => {
                    this.state.minPrice = null;
                    const input = document.getElementById('filter-min-price');
                    if (input) input.value = '';
                }
            });
        }

        if (this.state.maxPrice !== null) {
            chips.push({
                label: `Max: $${this.state.maxPrice}`,
                remove: () => {
                    this.state.maxPrice = null;
                    const input = document.getElementById('filter-max-price');
                    if (input) input.value = '';
                }
            });
        }

        if (this.state.inStockOnly) {
            chips.push({
                label: 'In Stock Only',
                remove: () => {
                    this.state.inStockOnly = false;
                    const checkbox = document.getElementById('filter-instock');
                    if (checkbox) checkbox.checked = false;
                }
            });
        }

        if (chips.length === 0) {
            container.style.display = 'none';
            container.innerHTML = '';
            return;
        }

        container.style.display = 'flex';
        container.innerHTML = `
            <span style="font-weight: 600; color: var(--text-secondary);">Active Filters:</span>
            ${chips.map((c, i) => `
                <span class="filter-chip">
                    ${escapeHtml(c.label)}
                    <button class="filter-chip-remove" data-chip-index="${i}" title="Remove filter">&times;</button>
                </span>
            `).join('')}
            <button id="btn-clear-all-chips" class="clear-all-btn">Reset all</button>
        `;

        container.querySelectorAll('.filter-chip-remove').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const index = Number(e.currentTarget.dataset.chipIndex);
                if (chips[index]) {
                    chips[index].remove();
                    this.state.page = 0;
                    this.loadProducts();
                }
            });
        });

        document.getElementById('btn-clear-all-chips')?.addEventListener('click', () => {
            this.resetAllFilters();
        });
    }

    resetAllFilters() {
        this.state.query = '';
        this.state.categoryId = null;
        this.state.minPrice = null;
        this.state.maxPrice = null;
        this.state.inStockOnly = false;
        this.state.page = 0;

        const searchInput = document.getElementById('search-query');
        if (searchInput) searchInput.value = '';
        this.toggleClearButton(false);

        const minInput = document.getElementById('filter-min-price');
        if (minInput) minInput.value = '';

        const maxInput = document.getElementById('filter-max-price');
        if (maxInput) maxInput.value = '';

        const inStockCheckbox = document.getElementById('filter-instock');
        if (inStockCheckbox) inStockCheckbox.checked = false;

        document.querySelectorAll('.category-pill').forEach(p => p.classList.remove('active'));
        document.querySelector('.category-pill[data-category-id=""]')?.classList.add('active');

        this.loadProducts();
    }

    renderPagination(pageData) {
        const container = document.getElementById('pagination-controls');
        if (!container) return;

        if (pageData.totalPages <= 1) {
            container.innerHTML = '';
            return;
        }

        const currentPage = pageData.pageNumber;
        const totalPages = pageData.totalPages;

        let buttonsHtml = `
            <button class="page-btn" ${pageData.first ? 'disabled' : ''} data-page="${currentPage - 1}">
                &larr; Prev
            </button>
        `;

        // Render page window (up to 5 pages)
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);

        if (startPage > 0) {
            buttonsHtml += `<button class="page-btn" data-page="0">1</button>`;
            if (startPage > 1) {
                buttonsHtml += `<span style="padding: 0 0.25rem; color: var(--text-muted);">...</span>`;
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            const isActive = i === currentPage;
            buttonsHtml += `
                <button class="page-btn ${isActive ? 'active' : ''}" data-page="${i}">
                    ${i + 1}
                </button>
            `;
        }

        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                buttonsHtml += `<span style="padding: 0 0.25rem; color: var(--text-muted);">...</span>`;
            }
            buttonsHtml += `<button class="page-btn" data-page="${totalPages - 1}">${totalPages}</button>`;
        }

        buttonsHtml += `
            <button class="page-btn" ${pageData.last ? 'disabled' : ''} data-page="${currentPage + 1}">
                Next &rarr;
            </button>
        `;

        container.innerHTML = `
            <div class="pagination-meta">
                Showing page <strong>${currentPage + 1}</strong> of <strong>${totalPages}</strong>
            </div>
            <div class="pagination">
                ${buttonsHtml}
            </div>
        `;

        container.querySelectorAll('.page-btn:not(:disabled)').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const targetPage = Number(e.currentTarget.dataset.page);
                if (!isNaN(targetPage) && targetPage !== this.state.page) {
                    this.state.page = targetPage;
                    this.loadProducts();
                    window.scrollTo({ top: 380, behavior: 'smooth' });
                }
            });
        });
    }

    updateCatalogMeta(pageData) {
        const meta = document.getElementById('catalog-meta');
        if (!meta) return;

        const total = pageData.totalElements;
        const count = pageData.content.length;
        if (total === 0) {
            meta.textContent = '0 products found';
        } else {
            const start = pageData.pageNumber * pageData.pageSize + 1;
            const end = start + count - 1;
            meta.textContent = `Showing ${start}–${end} of ${total} products`;
        }
    }

    openQuickView(product) {
        const modal = document.getElementById('quickview-modal');
        const modalContent = document.getElementById('modal-product-content');
        if (!modal || !modalContent) return;

        const price = Number(product.sellingPrice).toFixed(2);
        const imgUrl = product.imageUrl || PLACEHOLDER_IMAGE;

        let stockStatus = '';
        if (!product.inStock) {
            stockStatus = '<span class="stock-pill out-of-stock">Out of Stock</span>';
        } else if (product.lowStock) {
            stockStatus = `<span class="stock-pill low-stock">Low Stock (${product.stockQuantity} remaining)</span>`;
        } else {
            stockStatus = `<span class="stock-pill in-stock">In Stock (${product.stockQuantity} available)</span>`;
        }

        modalContent.innerHTML = `
            <div>
                <img src="${imgUrl}" 
                     alt="${escapeHtml(product.name)}" 
                     style="width: 100%; height: 280px; object-fit: contain; background: #f8fafc; border-radius: var(--radius-md); border: 1px solid var(--border-color);"
                     onerror="this.onerror=null; this.src='${PLACEHOLDER_IMAGE}'">
            </div>
            <div style="display: flex; flex-direction: column;">
                <span class="product-category" style="font-size: 0.8rem; margin-bottom: 0.25rem;">
                    ${escapeHtml(product.categoryName || 'General')}
                </span>
                <h2 style="font-size: 1.4rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.5rem;">
                    ${escapeHtml(product.name)}
                </h2>
                <div style="font-size: 0.8rem; font-family: monospace; color: var(--text-muted); margin-bottom: 0.75rem;">
                    SKU: ${escapeHtml(product.sku)}
                </div>
                <div style="display: flex; align-items: baseline; gap: 1rem; margin-bottom: 1rem;">
                    <span style="font-size: 1.75rem; font-weight: 800; color: var(--primary);">$${price}</span>
                    ${stockStatus}
                </div>
                <p style="font-size: 0.95rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 1.5rem;">
                    ${escapeHtml(product.description || 'Full product specification and details.')}
                </p>
                <div style="margin-top: auto; display: flex; gap: 0.75rem;">
                    <a href="/product.html?id=${product.id}" class="btn btn-primary" style="flex: 1;">
                        View Full Details Page &rarr;
                    </a>
                </div>
            </div>
        `;

        modal.classList.add('open');
        document.body.style.overflow = 'hidden';
    }

    closeQuickView() {
        const modal = document.getElementById('quickview-modal');
        if (modal) {
            modal.classList.remove('open');
            document.body.style.overflow = '';
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

// Auto-initialize when loaded
export const catalog = new CatalogManager();
document.addEventListener('DOMContentLoaded', () => {
    catalog.init();
});
