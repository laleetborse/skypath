# SkyPath Backend

Spring Boot REST API that powers the flight connection search engine. Loads a static flight dataset into memory and uses **BFS graph traversal** to find valid itineraries with timezone-aware connection validation.

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Build tool | Maven (wrapper included) |
| Container | Eclipse Temurin JDK/JRE |
| Testing | JUnit 5 + Spring Boot Test |

---

## Running Locally

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

### Running Tests

```bash
./mvnw test
```

---

## Project Structure

```
src/main/java/com/skypath/backend/
├── controller/
│   ├── FlightSearchController.java    # GET /api/flights/search
│   └── AirportController.java         # GET /api/airports, /api/airports/search
│
├── service/
│   ├── FlightSearchService.java       # BFS graph traversal
│   ├── ConnectionValidator.java       # Layover rules (domestic/intl, min/max)
│   └── TimeService.java              # Local time → UTC conversion
│
├── repository/
│   └── FlightDataRepository.java      # Loads flights.json at startup
│
├── model/
│   ├── Airport.java                   # code, name, city, country, timezone
│   ├── Flight.java                    # flightNumber, origin, dest, times, price
│   ├── Itinerary.java                 # segments, totalDuration, totalPrice
│   └── FlightSegment.java            # flight + layoverMinutes
│
└── exception/
    ├── GlobalExceptionHandler.java    # @RestControllerAdvice error mapping
    ├── AirportNotFoundException.java  # → 404
    ├── FlightSearchException.java     # → 400
    └── ApiErrorResponse.java          # Structured error body
```

---

## Algorithm: BFS Graph Search

The airport network is modeled as a **directed graph**:

- **Nodes** = airports (25 airports in the dataset)
- **Edges** = flights (directed, from origin to destination)
- **Adjacency list** = `Map<String, List<Flight>>` indexed by origin IATA code

### Search Flow

```
Input: origin="JFK", destination="LAX", date="2024-03-15"

1. VALIDATE
   ├── origin exists in airport map?     → 404 if not
   ├── destination exists in airport map? → 404 if not
   ├── origin == destination?            → 400 error
   └── date is valid ISO format?         → 400 if not

2. SEED BFS QUEUE
   ├── Get all flights where flight.origin == "JFK"
   ├── Filter: flight.departureDate == 2024-03-15
   └── Queue ← [ [SP101], [SP102], ... ]  (each is a path of length 1)

3. BFS LOOP
   while queue not empty:
     path = queue.poll()

     if path.length > 3 → skip (max 2 stops)

     if path.last().destination == "LAX":
       results.add(buildItinerary(path))
     else:
       for each nextFlight from path.last().destination:
         if isValidConnection(path.last(), nextFlight):
           queue.add(path + nextFlight)

4. SORT by totalDurationMinutes ascending

5. RETURN results
```

### Complexity

With ~260 flights across 25 airports and a max depth of 3:
- Worst case explores O(F * B^2) paths where F = flights from origin on date, B = avg branching factor
- In practice, the connection constraints (45–360 min layover) heavily prune the search space

---

## Connection Validation

`ConnectionValidator.isValidConnection(prev, next)`:

```
1. prev.destination == next.origin?          → must match (no airport changes)
2. Convert both times to UTC:
     arrivalUTC  = toUTC(prev.arrivalTime, arrivalAirport.timezone)
     departureUTC = toUTC(next.departureTime, departureAirport.timezone)
3. layover = departureUTC - arrivalUTC       → must be >= 0
4. domestic = (prev.origin.country == connection.country == next.destination.country)
     All three airports must be in the same country — otherwise international rules apply.
     e.g. JFK(US)→ORD(US)→LAX(US) = domestic, JFK(US)→LHR(GB)→CDG(FR) = international
5. minLayover = domestic ? 45 min : 90 min
6. layover >= minLayover AND layover <= 360 min
```

### Why UTC Conversion Matters

Times in `flights.json` are in **local airport time**. Without conversion:
- A 2h flight from JFK (ET, UTC-4) to ORD (CT, UTC-5) would appear as 2h but is actually 3h wall-clock.
- A SYD → LAX flight crossing the date line would show negative duration.

`TimeService.toUTC()` converts `LocalDateTime` → `ZonedDateTime` (using the airport's IANA timezone) → UTC, ensuring all comparisons are in absolute time.

---

## API Reference

### `GET /api/flights/search`

| Parameter | Type | Required | Example |
|-----------|------|----------|---------|
| `origin` | String | Yes | `JFK` |
| `destination` | String | Yes | `LAX` |
| `date` | String | Yes | `2024-03-15` |

**200 Response:**
```json
[
  {
    "segments": [
      {
        "flight": {
          "flightNumber": "SP101",
          "airline": "SkyPath Airways",
          "origin": "JFK",
          "destination": "LAX",
          "departureTime": "2024-03-15T08:30:00",
          "arrivalTime": "2024-03-15T11:45:00",
          "price": 299.0,
          "aircraft": "A320"
        },
        "layoverMinutes": 0
      }
    ],
    "totalDurationMinutes": 495,
    "totalPrice": 299.0
  }
]
```

**Error responses:**

| Status | Body | When |
|--------|------|------|
| 400 | `{ "message": "Origin and destination cannot be the same airport: JFK" }` | Same airport |
| 400 | `{ "message": "Invalid date format..." }` | Bad date |
| 400 | `{ "message": "Missing required parameter: origin" }` | Missing param |
| 404 | `{ "message": "Airport not found: XXX" }` | Unknown IATA code |

---

## Docker

Multi-stage build for minimal image size:

```dockerfile
# Stage 1: Build with JDK
FROM eclipse-temurin:17-jdk AS build
# ... compile with Maven

# Stage 2: Run with JRE only
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
```

The backend container **only exposes port 8080 internally** on the Docker bridge network — it is not accessible from the host. All external traffic routes through the frontend's Nginx reverse proxy.

---

## Test Coverage

Tests are in `src/test/java/com/skypath/backend/`:

| Test Class | What It Covers |
|------------|----------------|
| `FlightSearchServiceTest` | All 6 spec test cases (JFK→LAX, SFO→NRT, BOS→SEA, JFK→JFK, XXX→LAX, SYD→LAX), sorting, pricing, endpoint correctness, date validation, circular route prevention |
| `ConnectionValidatorTest` | Domestic/international layover minimums, max layover, airport mismatch, negative layover, timezone-aware calculations, boundary values (44/45 min, 89/90 min, 360/361 min) |
