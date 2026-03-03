# SkyPath Frontend

React single-page application for searching flight itineraries. Built with **Vite**, **Tailwind CSS v4**, and **Framer Motion**, served via **Nginx** in production.

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Framework | React 19 |
| Build tool | Vite 7 |
| Styling | Tailwind CSS 4 (with custom design tokens) |
| Animations | Framer Motion |
| Icons | React Icons (Feather + Heroicons) |
| HTTP client | Native `fetch` API |
| Production server | Nginx (Alpine) |

---

## Running Locally

```bash
npm install
npm run dev
```

Opens on `http://localhost:5173`. API requests go to `http://localhost:8080/api` by default.

To point at a different backend:
```bash
VITE_API_BASE_URL=http://your-backend:8080/api npm run dev
```

---

## Project Structure

```
src/
├── App.jsx                    # Root layout (Navbar + Hero + Footer)
├── main.jsx                   # React entry point
├── index.css                  # Global styles, design tokens, dark mode
│
├── components/
│   ├── Navbar.jsx             # Top bar with dark mode toggle
│   ├── Hero.jsx               # Landing section with search form
│   ├── FlightSearch.jsx       # Search form (origin, destination, date)
│   ├── AirportInput.jsx       # Autocomplete airport selector
│   ├── FlightResults.jsx      # Itinerary list + expanded segment view
│   └── Footer.jsx             # Page footer
│
├── services/
│   └── api.js                 # API client (searchFlights, searchAirports)
│
└── hooks/
    └── useTheme.js            # Dark/light mode state with localStorage
```

---

## Key Components

### `FlightSearch.jsx` — Search Form

Manages the search state (origin, destination, date) and validates before calling the API:

- Both origin and destination must be selected
- Origin and destination cannot be the same airport
- Date must be selected
- Displays inline error messages for validation failures and API errors

### `AirportInput.jsx` — Autocomplete

Debounced search input (200ms) that calls `/api/airports/search?q=` and shows a dropdown of matching airports. Handles:

- Keyboard and click selection
- Click-outside dismissal
- Clear/reset when an airport is already selected
- Loading spinner during search

### `FlightResults.jsx` — Results Display

Renders the itinerary list with:

- Departure/arrival times and airport codes
- Visual route line with connection dots
- Direct / 1-stop / 2-stop labels (color-coded)
- Total duration and price
- Expandable detail view showing each segment with layover info
- Loading spinner and empty state

---

## Styling Approach

### Design Tokens

Custom CSS properties in `index.css` provide semantic color tokens that swap between light and dark mode:

```css
:root {
  --_surface: #ffffff;
  --_fg: #0f172a;
  --_border: #e2e8f0;
  /* ... */
}

.dark {
  --_surface: #09090b;
  --_fg: #f4f4f5;
  --_border: #27272a;
  /* ... */
}
```

These are registered as Tailwind theme values (`--color-surface`, `--color-fg`, etc.) so components use classes like `bg-surface`, `text-fg`, `border-border`.

### Dark Mode

Controlled via the `useTheme` hook. The `dark` class is toggled on `<html>` and persisted in `localStorage`.

---

## Pagination & Filtering — UI-Side

The backend returns all matching itineraries in a single response. Since the dataset is bounded (~260 flights), result sets are small enough to handle entirely on the client.

**Current approach:** All results rendered in a scrollable list.

**With more time, we would add:**
- Client-side filtering by number of stops, price range, airline
- Server-side pagination (`?page=1&size=20`) for larger datasets
- Virtual scrolling for very long result lists

The decision to keep this in the UI was deliberate — it avoids extra network round-trips for filter interactions and keeps the API contract simple. For a production system with thousands of flights, server-side pagination would be necessary.

---

## Docker (Production)

Multi-stage Dockerfile:

```
Stage 1: node:20-alpine
  → npm ci + npm run build
  → Produces /app/dist (static files)

Stage 2: nginx:alpine
  → Copies dist to /usr/share/nginx/html
  → Copies nginx.conf for routing
```

### Nginx Configuration

```
/ → serves React SPA (with SPA fallback: try_files → /index.html)
/api/* → reverse proxy to http://backend:8080/api/
*.js, *.css → 1 year cache with immutable header
```

The Nginx layer acts as an **API gateway** — the browser only talks to Nginx on port 80 (mapped to host 3000). API requests are transparently proxied to the backend container over Docker's internal bridge network. This eliminates CORS entirely.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Backend API base URL (dev only; production uses Nginx proxy) |
