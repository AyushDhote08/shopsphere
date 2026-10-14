/**
 * ShopSphere Authentication Module
 * Manages user authentication lifecycle and updates dynamic UI elements.
 */
import { api } from './api.js';

export const auth = {
    async getCurrentUser() {
        try {
            const res = await api.get('/api/auth/me');
            return res ? res.data : null;
        } catch (err) {
            return null;
        }
    },

    async login(email, password) {
        return await api.post('/api/auth/login', { email, password });
    },

    async register(fullName, email, password, phone) {
        return await api.post('/api/auth/register', { fullName, email, password, phone });
    },

    async logout() {
        await api.post('/api/auth/logout', {});
        window.location.href = '/login.html';
    },

    async updateNavbar() {
        const authContainer = document.getElementById('navbar-auth');
        const navLinks = document.querySelector('.nav-links');

        const user = await this.getCurrentUser();

        // Dynamically add My Orders and Cart links to navLinks if present
        if (navLinks) {
            let linksHtml = `
                <li><a href="/index.html" class="nav-link">Catalog</a></li>
            `;
            if (user) {
                if (user.role === 'ROLE_ADMIN') {
                    linksHtml += `
                        <li><a href="/admin.html" class="nav-link" style="color: #6366f1; font-weight: 700;">Admin Console</a></li>
                    `;
                }
                linksHtml += `
                    <li><a href="/orders.html" class="nav-link">My Orders</a></li>
                `;
            }
            linksHtml += `
                <li>
                    <a href="/cart.html" class="nav-link" style="display: flex; align-items: center; gap: 0.35rem;">
                        <span>Cart</span>
                        <span id="nav-cart-badge" class="badge-count" style="display: none;">0</span>
                    </a>
                </li>
                <li><a href="/swagger-ui.html" target="_blank" class="nav-link">API Docs</a></li>
            `;
            navLinks.innerHTML = linksHtml;
        }

        if (authContainer) {
            if (user) {
                const isAdmin = user.role === 'ROLE_ADMIN';
                authContainer.innerHTML = `
                    <div class="user-badge">
                        <span>${escapeHtml(user.fullName)}</span>
                        <span class="role-pill ${isAdmin ? 'admin' : ''}">${isAdmin ? 'ADMIN' : 'CUSTOMER'}</span>
                    </div>
                    <button id="btn-logout" class="btn btn-outline btn-sm">Logout</button>
                `;

                document.getElementById('btn-logout').addEventListener('click', async (e) => {
                    e.preventDefault();
                    await auth.logout();
                });
            } else {
                authContainer.innerHTML = `
                    <a href="/login.html" class="btn btn-outline btn-sm">Sign In</a>
                    <a href="/register.html" class="btn btn-primary btn-sm">Register</a>
                `;
            }
        }

        if (user) {
            await this.updateCartBadge();
        }
    },

    async updateCartBadge() {
        const badge = document.getElementById('nav-cart-badge');
        if (!badge) return;
        try {
            const res = await api.get('/api/cart');
            if (res && res.data) {
                const count = res.data.totalItems || 0;
                badge.textContent = count;
                badge.style.display = count > 0 ? 'inline-flex' : 'none';
            }
        } catch (err) {
            badge.style.display = 'none';
        }
    }
};

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, function(m) {
        return {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        }[m];
    });
}

// Auto-initialize navbar on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    auth.updateNavbar();
});
