// API helper — stores JWT and provides fetch wrappers
const api = {
    token: null,

    setToken(t) { this.token = t; localStorage.setItem('token', t); },
    getToken() { return this.token || localStorage.getItem('token'); },
    clearToken() { this.token = null; localStorage.removeItem('token'); },

    async request(method, url, body) {
        const opts = { method, headers: { 'Content-Type': 'application/json' } };
        const t = this.getToken();
        if (t) opts.headers['Authorization'] = 'Bearer ' + t;
        if (body) opts.body = JSON.stringify(body);
        const res = await fetch(url, opts);
        if (res.status === 401) { this.clearToken(); window.location.href = '/login'; return; }
        if (!res.ok) {
            const errBody = await res.json().catch(() => ({ error: res.statusText }));
            const e = new Error(errBody.error || JSON.stringify(errBody));
            e.status = res.status;
            e.body = errBody;
            throw e;
        }
        if (res.status === 204) return null;
        return res.json();
    },

    get(url) { return this.request('GET', url); },
    post(url, body) { return this.request('POST', url, body); },
    put(url, body) { return this.request('PUT', url, body); },
    del(url) { return this.request('DELETE', url); }
};
