const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

async function request(url) {
  const res = await fetch(url);
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body.message) message = body.message;
    } catch {
      // response wasn't JSON, use default message
    }
    throw new Error(message);
  }
  return res.json();
}

export async function searchAirports(query) {
  if (!query || query.trim().length < 1) return [];
  return request(`${API_BASE}/airports/search?q=${encodeURIComponent(query.trim())}`);
}

export async function getAllAirports() {
  return request(`${API_BASE}/airports`);
}

export async function searchFlights(origin, destination, date) {
  const params = new URLSearchParams({ origin, destination, date });
  return request(`${API_BASE}/flights/search?${params}`);
}
