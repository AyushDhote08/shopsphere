/**
 * ShopSphere API Client
 * Vanilla JavaScript ES6 module providing CSRF header attachment,
 * session credential inclusion, and unified error handling.
 */

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

export async function request(endpoint, options = {}) {
    const defaultHeaders = {
        'Accept': 'application/json'
    };

    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
        defaultHeaders['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }

    // Attach CSRF token for state-mutating requests
    const method = (options.method || 'GET').toUpperCase();
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        const csrfToken = getCookie('XSRF-TOKEN');
        if (csrfToken) {
            defaultHeaders['X-XSRF-TOKEN'] = decodeURIComponent(csrfToken);
        }
    }

    options.headers = {
        ...defaultHeaders,
        ...options.headers
    };

    // Include same-origin session cookies
    options.credentials = 'same-origin';

    try {
        const response = await fetch(endpoint, options);

        if (response.status === 204) {
            return null;
        }

        const data = await response.json();

        if (!response.ok) {
            const error = new Error(data.message || 'API request failed');
            error.status = response.status;
            error.data = data;
            throw error;
        }

        return data;
    } catch (err) {
        if (!err.status) {
            console.error('Network or fetch error:', err);
        }
        throw err;
    }
}

export const api = {
    get: (url) => request(url, { method: 'GET' }),
    post: (url, body) => request(url, { method: 'POST', body }),
    put: (url, body) => request(url, { method: 'PUT', body }),
    delete: (url) => request(url, { method: 'DELETE' })
};
