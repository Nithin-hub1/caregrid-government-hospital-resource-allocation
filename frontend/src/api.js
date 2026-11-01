// CareGrid Central API Client with JWT Bearer Authentication

const TOKEN_KEY = "caregrid_jwt_token";
const USER_KEY = "caregrid_user_data";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(USER_KEY);
  }
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export async function authFetch(url, options = {}) {
  const token = getToken();
  const headers = {
    ...(options.headers || {}),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  if (options.body && !(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    clearAuth();
    window.dispatchEvent(new CustomEvent("caregrid:unauthorized"));
  }

  return response;
}

export async function loginApi(usernameOrEmail, password) {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ usernameOrEmail, password }),
  });

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(data.error || data.message || `Login failed (${response.status})`);
  }

  setToken(data.token);
  setUser({
    id: data.id,
    username: data.username,
    email: data.email,
    role: data.role,
    fullName: data.fullName,
    hospitalId: data.hospitalId,
    hospitalName: data.hospitalName,
  });

  return data;
}

export async function logoutApi() {
  try {
    await authFetch("/api/auth/logout", { method: "POST" });
  } catch (err) {
    console.warn("Logout request failed:", err);
  } finally {
    clearAuth();
  }
}

export async function fetchCurrentUserApi() {
  const response = await authFetch("/api/auth/me");
  if (!response.ok) {
    throw new Error("Failed to fetch user info");
  }
  const user = await response.json();
  setUser(user);
  return user;
}
