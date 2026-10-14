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
        if (!authContainer) return;

        const user = await this.getCurrentUser();

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
