package com.skypath.backend.service;

import com.skypath.backend.exception.AirportNotFoundException;
import com.skypath.backend.model.Flight;
import com.skypath.backend.model.FlightSegment;
import com.skypath.backend.model.Itinerary;
import com.skypath.backend.repository.FlightDataRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FlightSearchServiceTest {

    static FlightDataRepository repository;
    static TimeService timeService;
    static ConnectionValidator validator;
    static FlightSearchService service;

    @BeforeAll
    static void setUp() throws Exception {
        repository = new FlightDataRepository();
        repository.loadData();
        timeService = new TimeService();
        validator = new ConnectionValidator(timeService, repository);
        service = new FlightSearchService(repository, validator, timeService);
    }

    // ──────────────────────────────────────────────
    // Test Case 1: JFK → LAX, 2024-03-15
    // Expected: Returns direct flights AND multi-stop options
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("TC1: JFK → LAX — direct flights AND multi-stop")
    class JfkToLax {

        final List<Itinerary> results = service.search("JFK", "LAX", "2024-03-15");

        @Test
        @DisplayName("returns at least one itinerary")
        void returnsResults() {
            assertFalse(results.isEmpty(), "Should find at least one route JFK→LAX");
        }

        @Test
        @DisplayName("contains at least one direct flight (1 segment)")
        void hasDirectFlight() {
            boolean hasDirect = results.stream()
                    .anyMatch(it -> it.getSegments().size() == 1);
            assertTrue(hasDirect, "Should include a direct JFK→LAX flight");
        }

        @Test
        @DisplayName("contains at least one multi-stop option (>1 segment)")
        void hasMultiStop() {
            boolean hasMulti = results.stream()
                    .anyMatch(it -> it.getSegments().size() > 1);
            assertTrue(hasMulti, "Should include connecting routes JFK→…→LAX");
        }

        @Test
        @DisplayName("all itineraries start at JFK and end at LAX")
        void correctEndpoints() {
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                assertEquals("JFK", segs.get(0).getFlight().getOrigin(),
                        "First segment must depart from JFK");
                assertEquals("LAX", segs.get(segs.size() - 1).getFlight().getDestination(),
                        "Last segment must arrive at LAX");
            }
        }

        @Test
        @DisplayName("results are sorted by total duration ascending")
        void sortedByDuration() {
            for (int i = 1; i < results.size(); i++) {
                assertTrue(
                        results.get(i).getTotalDurationMinutes()
                                >= results.get(i - 1).getTotalDurationMinutes(),
                        "Results should be sorted by total travel time (shortest first)");
            }
        }

        @Test
        @DisplayName("no itinerary exceeds 3 segments (max 2 stops)")
        void maxTwoStops() {
            for (Itinerary it : results) {
                assertTrue(it.getSegments().size() <= 3,
                        "Maximum 2 stops means at most 3 flight segments");
            }
        }

        @Test
        @DisplayName("total price equals sum of segment prices")
        void priceIsConsistent() {
            for (Itinerary it : results) {
                double segmentSum = it.getSegments().stream()
                        .mapToDouble(s -> s.getFlight().getPrice())
                        .sum();
                assertEquals(segmentSum, it.getTotalPrice(), 0.01,
                        "Total price must equal sum of flight segment prices");
            }
        }

        @Test
        @DisplayName("total duration is positive")
        void positiveDuration() {
            for (Itinerary it : results) {
                assertTrue(it.getTotalDurationMinutes() > 0,
                        "Total duration must be positive");
            }
        }
    }

    // ──────────────────────────────────────────────
    // Test Case 2: SFO → NRT, 2024-03-15
    // Expected: International route — 90-minute minimum layover applies
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("TC2: SFO → NRT — international 90-min minimum layover")
    class SfoToNrt {

        final List<Itinerary> results = service.search("SFO", "NRT", "2024-03-15");

        @Test
        @DisplayName("returns results for international route")
        void returnsResults() {
            assertFalse(results.isEmpty(), "Should find routes SFO→NRT");
        }

        @Test
        @DisplayName("connecting itineraries respect 90-min international layover minimum")
        void internationalLayoverMinimum() {
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                if (segs.size() < 2) continue;

                for (int i = 0; i < segs.size() - 1; i++) {
                    Flight arriving = segs.get(i).getFlight();
                    Flight departing = segs.get(i + 1).getFlight();

                    String connAirport = arriving.getDestination();
                    assertEquals(connAirport, departing.getOrigin(),
                            "Connection airport must match");

                    var arrAirport = repository.getAirports().get(arriving.getDestination());
                    var depAirport = repository.getAirports().get(departing.getOrigin());

                    var arrUTC = timeService.toUTC(arriving.getArrivalTime(), arrAirport);
                    var depUTC = timeService.toUTC(departing.getDepartureTime(), depAirport);
                    long layover = timeService.minutesBetween(arrUTC, depUTC);

                    boolean domestic = arrAirport.getCountry().equals(depAirport.getCountry());
                    long minLayover = domestic ? 45 : 90;

                    assertTrue(layover >= minLayover,
                            String.format("Layover at %s is %d min, minimum required is %d min (%s)",
                                    connAirport, layover, minLayover,
                                    domestic ? "domestic" : "international"));
                    assertTrue(layover <= 360,
                            String.format("Layover at %s is %d min, max allowed is 360 min",
                                    connAirport, layover));
                }
            }
        }

        @Test
        @DisplayName("all itineraries start at SFO and end at NRT")
        void correctEndpoints() {
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                assertEquals("SFO", segs.get(0).getFlight().getOrigin());
                assertEquals("NRT", segs.get(segs.size() - 1).getFlight().getDestination());
            }
        }
    }

    // ──────────────────────────────────────────────
    // Test Case 3: BOS → SEA, 2024-03-15
    // Expected: No direct flight — must find connections
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("TC3: BOS → SEA — no direct flight, connections only")
    class BosToSea {

        final List<Itinerary> results = service.search("BOS", "SEA", "2024-03-15");

        @Test
        @DisplayName("returns at least one itinerary")
        void returnsResults() {
            assertFalse(results.isEmpty(), "Should find connecting routes BOS→…→SEA");
        }

        @Test
        @DisplayName("no direct flight exists (all results have >1 segment)")
        void noDirectFlight() {
            for (Itinerary it : results) {
                assertTrue(it.getSegments().size() > 1,
                        "BOS→SEA has no direct flight — all itineraries should be connections");
            }
        }

        @Test
        @DisplayName("segments form a connected chain")
        void segmentsConnected() {
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                for (int i = 0; i < segs.size() - 1; i++) {
                    assertEquals(
                            segs.get(i).getFlight().getDestination(),
                            segs.get(i + 1).getFlight().getOrigin(),
                            "Each segment's destination must match the next segment's origin");
                }
            }
        }

        @Test
        @DisplayName("all itineraries start at BOS and end at SEA")
        void correctEndpoints() {
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                assertEquals("BOS", segs.get(0).getFlight().getOrigin());
                assertEquals("SEA", segs.get(segs.size() - 1).getFlight().getDestination());
            }
        }
    }

    // ──────────────────────────────────────────────
    // Test Case 4: JFK → JFK, 2024-03-15
    // Expected: Empty results or validation error
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("TC4: JFK → JFK — same origin and destination")
    class SameAirport {

        @Test
        @DisplayName("throws FlightSearchException when origin equals destination")
        void throwsForSameAirport() {
            var ex = assertThrows(
                    com.skypath.backend.exception.FlightSearchException.class,
                    () -> service.search("JFK", "JFK", "2024-03-15"),
                    "Same origin and destination should throw FlightSearchException");
            assertTrue(ex.getMessage().contains("JFK"),
                    "Error message should mention the airport code");
        }
    }

    // ──────────────────────────────────────────────
    // Test Case 5: XXX → LAX, 2024-03-15
    // Expected: Invalid airport — graceful error handling
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("TC5: XXX → LAX — invalid airport code")
    class InvalidAirport {

        @Test
        @DisplayName("throws AirportNotFoundException for invalid origin")
        void invalidOriginThrows() {
            assertThrows(AirportNotFoundException.class,
                    () -> service.search("XXX", "LAX", "2024-03-15"),
                    "Invalid origin airport code should throw AirportNotFoundException");
        }

        @Test
        @DisplayName("throws AirportNotFoundException for invalid destination")
        void invalidDestinationThrows() {
            assertThrows(AirportNotFoundException.class,
                    () -> service.search("JFK", "ZZZ", "2024-03-15"),
                    "Invalid destination airport code should throw AirportNotFoundException");
        }

        @Test
        @DisplayName("exception contains the invalid airport code")
        void exceptionContainsCode() {
            AirportNotFoundException ex = assertThrows(
                    AirportNotFoundException.class,
                    () -> service.search("XXX", "LAX", "2024-03-15"));
            assertEquals("XXX", ex.getAirportCode());
            assertTrue(ex.getMessage().contains("XXX"));
        }
    }

    // ──────────────────────────────────────────────
    // Test Case 6: SYD → LAX, 2024-03-15
    // Expected: Date line crossing — arrival "before" departure in local time
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("TC6: SYD → LAX — date line crossing")
    class DateLineCrossing {

        final List<Itinerary> results = service.search("SYD", "LAX", "2024-03-15");

        @Test
        @DisplayName("returns results despite date line crossing")
        void returnsResults() {
            assertFalse(results.isEmpty(),
                    "Should find routes SYD→LAX even with date line crossing");
        }

        @Test
        @DisplayName("total duration is positive even when local arrival < local departure")
        void positiveDurationAcrossDateLine() {
            for (Itinerary it : results) {
                assertTrue(it.getTotalDurationMinutes() > 0,
                        "Duration must be positive — timezone-aware calculation required");
            }
        }

        @Test
        @DisplayName("duration is realistic for transpacific travel (>10h for direct)")
        void realisticDuration() {
            for (Itinerary it : results) {
                if (it.getSegments().size() == 1) {
                    assertTrue(it.getTotalDurationMinutes() >= 600,
                            "Direct SYD→LAX should be at least ~10 hours (600 min)");
                }
            }
        }

        @Test
        @DisplayName("all itineraries start at SYD and end at LAX")
        void correctEndpoints() {
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                assertEquals("SYD", segs.get(0).getFlight().getOrigin());
                assertEquals("LAX", segs.get(segs.size() - 1).getFlight().getDestination());
            }
        }
    }

    // ──────────────────────────────────────────────
    // General structural / connection-rule tests
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Connection rules validation across all routes")
    class ConnectionRules {

        @Test
        @DisplayName("no airport change during layover (same airport for arrival/departure)")
        void noAirportChange() {
            List<Itinerary> results = service.search("JFK", "LAX", "2024-03-15");
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                for (int i = 0; i < segs.size() - 1; i++) {
                    assertEquals(
                            segs.get(i).getFlight().getDestination(),
                            segs.get(i + 1).getFlight().getOrigin(),
                            "Passengers cannot change airports during a layover");
                }
            }
        }

        @Test
        @DisplayName("domestic connections have >= 45 min layover")
        void domesticMinLayover() {
            List<Itinerary> results = service.search("JFK", "LAX", "2024-03-15");
            assertLayoverBounds(results);
        }

        @Test
        @DisplayName("all layovers are <= 6 hours (360 min)")
        void maxLayover() {
            List<Itinerary> results = service.search("JFK", "LAX", "2024-03-15");
            assertLayoverBounds(results);
        }

        @Test
        @DisplayName("connection rules hold for international route SFO→NRT")
        void internationalConnectionRules() {
            List<Itinerary> results = service.search("SFO", "NRT", "2024-03-15");
            assertLayoverBounds(results);
        }

        @Test
        @DisplayName("connection rules hold for BOS→SEA connections")
        void bosSeaConnectionRules() {
            List<Itinerary> results = service.search("BOS", "SEA", "2024-03-15");
            assertLayoverBounds(results);
        }

        private void assertLayoverBounds(List<Itinerary> results) {
            for (Itinerary it : results) {
                List<FlightSegment> segs = it.getSegments();
                for (int i = 0; i < segs.size() - 1; i++) {
                    Flight arriving = segs.get(i).getFlight();
                    Flight departing = segs.get(i + 1).getFlight();

                    var arrAirport = repository.getAirports().get(arriving.getDestination());
                    var depAirport = repository.getAirports().get(departing.getOrigin());

                    var arrUTC = timeService.toUTC(arriving.getArrivalTime(), arrAirport);
                    var depUTC = timeService.toUTC(departing.getDepartureTime(), depAirport);
                    long layover = timeService.minutesBetween(arrUTC, depUTC);

                    boolean domestic = arrAirport.getCountry().equals(depAirport.getCountry());
                    long minLayover = domestic ? 45 : 90;

                    assertTrue(layover >= minLayover,
                            String.format("Layover at %s: %d min < required %d min (%s)",
                                    arriving.getDestination(), layover, minLayover,
                                    domestic ? "domestic" : "international"));
                    assertTrue(layover <= 360,
                            String.format("Layover at %s: %d min exceeds 360 min max",
                                    arriving.getDestination(), layover));
                }
            }
        }

        @Test
        @DisplayName("only departs on the requested date (2024-03-15)")
        void departureOnCorrectDate() {
            List<Itinerary> results = service.search("JFK", "LAX", "2024-03-15");
            for (Itinerary it : results) {
                var firstFlight = it.getSegments().get(0).getFlight();
                assertEquals(
                        java.time.LocalDate.of(2024, 3, 15),
                        firstFlight.getDepartureTime().toLocalDate(),
                        "First flight must depart on the requested date");
            }
        }
    }

    // ──────────────────────────────────────────────
    // Edge cases
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("invalid date format throws FlightSearchException")
        void invalidDateFormat() {
            assertThrows(
                    com.skypath.backend.exception.FlightSearchException.class,
                    () -> service.search("JFK", "LAX", "15-03-2024"),
                    "Non-ISO date should throw FlightSearchException");
        }

        @Test
        @DisplayName("date with no flights returns empty list")
        void noFlightsOnDate() {
            List<Itinerary> results = service.search("JFK", "LAX", "2025-01-01");
            assertTrue(results.isEmpty(),
                    "A date with no flights in the dataset should return empty results");
        }

        @Test
        @DisplayName("no circular routes in itineraries")
        void noCircularRoutes() {
            List<Itinerary> results = service.search("JFK", "LAX", "2024-03-15");
            for (Itinerary it : results) {
                List<String> airports = it.getSegments().stream()
                        .map(s -> s.getFlight().getOrigin())
                        .collect(Collectors.toList());
                Set<String> unique = Set.copyOf(airports);
                assertEquals(airports.size(), unique.size(),
                        "Itinerary should not visit the same airport twice as origin");
            }
        }
    }
}
