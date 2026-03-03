# SkyPath — Flight Connection Search Engine

A full-stack flight itinerary search engine that finds direct and multi-stop routes between airports, built with **Spring Boot** (backend) and **React + Vite** (frontend), orchestrated with **Docker Compose**.

---

## Quick Start

```bash
docker-compose up
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                    Docker Compose                            │
│                  (skypath bridge network)                    │
│                                                              │
│  ┌───────────────────────┐    ┌───────────────────────────┐  │
│  │      frontend         │    │        backend            │  │
│  │    (Nginx:80)         │    │   (Spring Boot:8080)      │  │
│  │                       │    │                           │  │
│  │  ┌─────────────────┐  │    │  ┌─────────────────────┐  │  │
│  │  │  React SPA      │  │    │  │  FlightSearchService│  │  │
│  │  │  (static files) │  │    │  │  (BFS Graph Search) │  │  │
│  │  └─────────────────┘  │    │  ├─────────────────────┤  │  │
│  │                       │    │  │ ConnectionValidator │  │  │
│  │  ┌─────────────────┐  │    │  │ TimeService (UTC)   │  │  │
│  │  │  Nginx Reverse  │───────│  ├─────────────────────┤  │  │
│  │  │  Proxy (/api/*) │  │    │  │ FlightDataRepository│  │  │
│  │  └─────────────────┘  │    │  │ (flights.json)      │  │  │
│  │                       │    │  └─────────────────────┘  │  │
│  └───────────────────────┘    └───────────────────────────┘  │
│         :3000 → host              :8080 (internal only)      │
└──────────────────────────────────────────────────────────────┘
```

### Docker & Networking

| Concept | Implementation |
|---------|----------------|
| **Bridge network** | A custom `skypath` bridge network connects both containers. The backend is **not exposed to the host** — only the frontend's port 3000 is published. |
| **Reverse proxy** | Nginx serves the React SPA and proxies `/api/*` requests to `http://backend:8080/api/`. Docker's internal DNS resolves the `backend` hostname within the bridge network. |
| **Why no direct backend port?** | The frontend Nginx acts as the single entry point (API gateway pattern). This avoids CORS issues, simplifies client configuration, and mirrors a production topology. |
| **Multi-stage builds** | Both Dockerfiles use multi-stage builds — build dependencies stay out of the final image, keeping it small and secure. |

---

## Core Algorithm: BFS Graph Traversal

The flight search models the airport network as a **directed graph** where airports are nodes and flights are edges. Finding itineraries is a **Breadth-First Search (BFS)** over this graph.

### How it works

```
Step 1: Build the graph
  flights.json → Map<origin, List<Flight>>
  Each airport is a node, each flight is a directed edge

Step 2: Seed the BFS queue
  Find all flights departing from ORIGIN on the requested DATE
  Queue ← [ [flight₁], [flight₂], ... ]

Step 3: Explore (BFS)
  while queue is not empty:
    path = queue.poll()
    lastFlight = path.last()

    if path.length > 3 → skip (max 2 stops = 3 segments)

    if lastFlight.destination == DESTINATION:
      → build itinerary, add to results

    else:
      for each nextFlight departing from lastFlight.destination:
        if isValidConnection(lastFlight, nextFlight):
          queue.add(path + nextFlight)

Step 4: Sort results by total travel time (shortest first)
```

### Why BFS?

- **Completeness**: BFS explores all paths level by level — direct flights (depth 1) are found before 1-stop (depth 2) before 2-stop (depth 3).
- **Bounded depth**: With a max of 3 segments, the search space is manageable without optimization.
- **Simplicity**: No priority queue or heuristic needed — we sort results after collection.

---

## Connection Validation Rules

Each potential connection is validated by `ConnectionValidator`:

| Rule | Constraint |
|------|-----------|
| **Airport match** | `prev.destination` must equal `next.origin` (no airport changes during layover) |
| **Time ordering** | Next flight must depart after previous flight arrives |
| **Domestic layover** | Minimum **45 minutes** (both flights within the same country) |
| **International layover** | Minimum **90 minutes** (flights cross a country boundary) |
| **Maximum layover** | **6 hours (360 minutes)** for all connections |

### Timezone Handling

All times in `flights.json` are **local airport times**. The `TimeService` converts them to UTC before comparing:

```
localTime → ZoneId(airport.timezone) → ZonedDateTime → UTC
```

This is critical for:
- **Cross-timezone domestic routes** (e.g., JFK/ET → ORD/CT — 1 hour difference)
- **International date line crossings** (e.g., SYD → LAX — arrival appears "before" departure in local time, but UTC calculation gives the correct positive duration)

---

## Tradeoffs & Decisions

### Pagination & Filtering — Client-Side vs Server-Side

Currently, **all matching itineraries are returned by the backend** and displayed in the frontend. Filtering (e.g., by stops, price range) and pagination happen on the client side.

**Why this approach:**
- The dataset is bounded (~260 flights, 25 airports) — result sets are small enough for a single response.
- Eliminates round-trips for filtering interactions (instant UI updates).
- Simpler API contract.

**With more time / larger datasets, we would:**
- Add server-side pagination (`?page=1&size=20`) with `Link` headers.
- Add query parameters for filtering (`?maxStops=1&maxPrice=500`).
- Cache search results with a TTL to avoid re-running BFS for paginated requests.

### In-Memory Data Store

The flight dataset is loaded from `flights.json` into Java `HashMap`s at startup — no database.

**Why:** The dataset is static and small. An in-memory map indexed by origin airport gives O(1) lookup for BFS neighbors.

**With more time:** For a real-world system with dynamic schedules, we'd use a database (PostgreSQL with GIS extensions or a graph database like Neo4j) and index on origin + departure date.

### No Circuit Avoidance in BFS

The current BFS does not explicitly track visited airports. This is acceptable because:
- Connections must have valid layovers (45–360 min), which naturally prevents tight loops.
- The depth cap of 3 segments limits the search space.

However, a visited set would improve performance with larger datasets.

---

## Project Structure

```
skypath/
├── docker-compose.yml        # Orchestrates both services
├── README.md                 # ← You are here
│
├── backend/                  # Spring Boot REST API (Java 17)
│   ├── dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/skypath/backend/
│       │   │   ├── controller/        # REST endpoints
│       │   │   ├── service/           # BFS search, validation, time
│       │   │   ├── repository/        # In-memory data store
│       │   │   ├── model/             # Airport, Flight, Itinerary
│       │   │   └── exception/         # Error handling
│       │   └── resources/
│       │       └── flights.json       # Dataset
│       └── test/                      # JUnit 5 tests
│
└── frontend/                 # React + Vite SPA
    ├── Dockerfile
    ├── nginx.conf            # Reverse proxy config
    ├── package.json
    └── src/
        ├── components/       # UI components
        ├── services/         # API client
        └── hooks/            # Custom React hooks
```

---

## API Endpoints

### `GET /api/flights/search`

Search for flight itineraries.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `origin` | string | Yes | 3-letter IATA code (e.g., `JFK`) |
| `destination` | string | Yes | 3-letter IATA code (e.g., `LAX`) |
| `date` | string | Yes | ISO 8601 date (`YYYY-MM-DD`) |

**Success (200):** Array of itineraries sorted by duration.

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Same origin and destination |
| 400 | Invalid date format |
| 400 | Missing required parameter |
| 404 | Unknown airport code |

### `GET /api/airports/search?q=`

Search airports by name, city, or code (autocomplete).

### `GET /api/airports`

Returns all airports in the dataset.

---

## Running Without Docker

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```
Runs on `http://localhost:8080`.

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`. API calls go to `localhost:8080` by default (configured via `VITE_API_BASE_URL`).

---

## Running Tests

```bash
cd backend
./mvnw test
```

Tests cover all 6 test scenarios from the spec, plus connection rule validation and edge cases.

---

## What I Would Improve With More Time

1. **Server-side pagination and filtering** — paginated API with cursor-based pagination for large result sets.
2. **Caching layer** — Redis or Caffeine cache keyed by (origin, destination, date) with short TTL.
3. **Layover duration in response segments** — currently `layoverMinutes` in each `FlightSegment` is not populated; would calculate per-segment layover for richer UI display.
4. **Price sorting option** — allow sorting by price in addition to duration.
5. **E2E tests** — Playwright or Cypress tests against the running Docker stack.
6. **CI/CD pipeline** — GitHub Actions for build, test, and Docker image publish.
7. **Rate limiting** — protect the search endpoint from abuse.
