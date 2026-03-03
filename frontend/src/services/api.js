const API_BASE = "http://localhost:8080/api";

export async function searchAirports(query) {
  if (!query || query.trim().length < 1) return [];
  const res = await fetch(`${API_BASE}/airports/search?q=${encodeURIComponent(query.trim())}`);
  if (!res.ok) throw new Error("Failed to search airports");
  return res.json();
}

export async function getAllAirports() {
  const res = await fetch(`${API_BASE}/airports`);
  if (!res.ok) throw new Error("Failed to fetch airports");
  return res.json();
}

export async function searchFlights(origin, destination, date) {
  const params = new URLSearchParams({ origin, destination, date });
  const res = await fetch(`${API_BASE}/flights/search?${params}`);
  if (!res.ok) throw new Error("Failed to search flights");
  return res.json();
}
