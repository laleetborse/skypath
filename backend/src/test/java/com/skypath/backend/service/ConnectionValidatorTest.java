package com.skypath.backend.service;

import com.skypath.backend.model.Airport;
import com.skypath.backend.model.Flight;
import com.skypath.backend.repository.FlightDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionValidatorTest {

    FlightDataRepository repository;
    TimeService timeService;
    ConnectionValidator validator;

    @BeforeEach
    void setUp() {
        repository = new FlightDataRepository();
        timeService = new TimeService();

        Airport jfk = makeAirport("JFK", "US", "America/New_York");
        Airport ord = makeAirport("ORD", "US", "America/Chicago");
        Airport lhr = makeAirport("LHR", "GB", "Europe/London");
        Airport cdg = makeAirport("CDG", "FR", "Europe/Paris");
        Airport lax = makeAirport("LAX", "US", "America/Los_Angeles");

        repository.getAirports().put("JFK", jfk);
        repository.getAirports().put("ORD", ord);
        repository.getAirports().put("LHR", lhr);
        repository.getAirports().put("CDG", cdg);
        repository.getAirports().put("LAX", lax);

        validator = new ConnectionValidator(timeService, repository);
    }

    // ──────────────────────────────────────────────
    // Airport mismatch
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Airport matching")
    class AirportMatching {

        @Test
        @DisplayName("rejects connection when airports don't match (e.g. JFK→LGA)")
        void rejectsAirportMismatch() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T08:00", "2024-03-15T09:30");
            Flight f2 = makeFlight("LHR", "CDG",
                    "2024-03-15T11:00", "2024-03-15T12:30");

            assertFalse(validator.isValidConnection(f1, f2),
                    "Connection must be rejected when prev.destination != next.origin");
        }
    }

    // ──────────────────────────────────────────────
    // Domestic layover rules (same country → 45 min minimum)
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Domestic layover (same country, 45 min min)")
    class DomesticLayover {

        @Test
        @DisplayName("accepts 45-minute domestic layover (exact minimum)")
        void accepts45MinDomestic() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T08:00", "2024-03-15T09:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T09:45", "2024-03-15T12:00");

            assertTrue(validator.isValidConnection(f1, f2),
                    "45-min domestic layover should be accepted");
        }

        @Test
        @DisplayName("rejects 30-minute domestic layover (below minimum)")
        void rejects30MinDomestic() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T08:00", "2024-03-15T09:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T09:30", "2024-03-15T12:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "30-min domestic layover should be rejected (minimum is 45)");
        }

        @Test
        @DisplayName("rejects 44-minute domestic layover (1 min short)")
        void rejects44MinDomestic() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T08:00", "2024-03-15T09:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T09:44", "2024-03-15T12:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "44-min domestic layover should be rejected");
        }

        @Test
        @DisplayName("accepts 2-hour domestic layover")
        void accepts2HourDomestic() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T08:00", "2024-03-15T09:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T11:00", "2024-03-15T14:00");

            assertTrue(validator.isValidConnection(f1, f2),
                    "2-hour domestic layover should be accepted");
        }
    }

    // ──────────────────────────────────────────────
    // International layover rules (different country → 90 min minimum)
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("International layover (different country, 90 min min)")
    class InternationalLayover {

        @Test
        @DisplayName("accepts 90-minute international layover (exact minimum)")
        void accepts90MinInternational() {
            Flight f1 = makeFlight("JFK", "LHR",
                    "2024-03-15T08:00", "2024-03-15T20:00");
            Flight f2 = makeFlight("LHR", "CDG",
                    "2024-03-15T21:30", "2024-03-15T23:00");

            assertTrue(validator.isValidConnection(f1, f2),
                    "90-min international layover should be accepted");
        }

        @Test
        @DisplayName("rejects 60-minute international layover (below minimum)")
        void rejects60MinInternational() {
            Flight f1 = makeFlight("JFK", "LHR",
                    "2024-03-15T08:00", "2024-03-15T20:00");
            Flight f2 = makeFlight("LHR", "CDG",
                    "2024-03-15T21:00", "2024-03-15T23:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "60-min international layover should be rejected (minimum is 90)");
        }

        @Test
        @DisplayName("rejects 89-minute international layover (1 min short)")
        void rejects89MinInternational() {
            Flight f1 = makeFlight("JFK", "LHR",
                    "2024-03-15T08:00", "2024-03-15T20:00");
            Flight f2 = makeFlight("LHR", "CDG",
                    "2024-03-15T21:29", "2024-03-15T23:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "89-min international layover should be rejected");
        }

        @Test
        @DisplayName("accepts 45-minute layover that would be valid domestic but fails international check")
        void rejects45MinInternational() {
            Flight f1 = makeFlight("JFK", "LHR",
                    "2024-03-15T08:00", "2024-03-15T20:00");
            Flight f2 = makeFlight("LHR", "CDG",
                    "2024-03-15T20:45", "2024-03-15T22:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "45-min international layover is valid for domestic but not international");
        }
    }

    // ──────────────────────────────────────────────
    // Maximum layover (6 hours = 360 minutes)
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Maximum layover (360 min / 6 hours)")
    class MaxLayover {

        @Test
        @DisplayName("accepts exactly 360-minute (6 hour) layover")
        void accepts360Min() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T06:00", "2024-03-15T07:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T13:00", "2024-03-15T15:00");

            assertTrue(validator.isValidConnection(f1, f2),
                    "Exactly 6-hour layover should be accepted");
        }

        @Test
        @DisplayName("rejects 361-minute layover (1 min over max)")
        void rejects361Min() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T06:00", "2024-03-15T07:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T13:01", "2024-03-15T15:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "361-min layover exceeds 6-hour max");
        }

        @Test
        @DisplayName("rejects 8-hour layover")
        void rejects8Hour() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T06:00", "2024-03-15T07:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T15:00", "2024-03-15T18:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "8-hour layover should be rejected");
        }
    }

    // ──────────────────────────────────────────────
    // Time ordering (arrival must be before departure)
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Time ordering")
    class TimeOrdering {

        @Test
        @DisplayName("rejects connection where next flight departs before previous arrives")
        void rejectsNegativeLayover() {
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T10:00", "2024-03-15T12:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T11:00", "2024-03-15T14:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "Connection where departure is before arrival should be rejected");
        }
    }

    // ──────────────────────────────────────────────
    // Timezone awareness
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Timezone-aware layover calculation")
    class TimezoneAwareness {

        @Test
        @DisplayName("accounts for timezone difference between ORD (Central) and JFK (Eastern)")
        void timezoneOffsetDomestic() {
            // JFK is UTC-5 (ET), ORD is UTC-6 (CT) in March (EDT/CDT haven't started yet on March 10, 2024)
            // Actually, DST starts March 10 2024 in US. So on March 15:
            // JFK = UTC-4, ORD = UTC-5
            // f1 arrives ORD at 09:00 CT = 14:00 UTC
            // f2 departs ORD at 09:44 CT = 14:44 UTC
            // layover = 44 min in real time → should be rejected (< 45 domestic)
            Flight f1 = makeFlight("JFK", "ORD",
                    "2024-03-15T08:00", "2024-03-15T09:00");
            Flight f2 = makeFlight("ORD", "LAX",
                    "2024-03-15T09:44", "2024-03-15T12:00");

            assertFalse(validator.isValidConnection(f1, f2),
                    "44 real minutes at ORD should be rejected for domestic (min 45)");
        }

        @Test
        @DisplayName("correctly handles LHR (GMT) to CDG (CET) timezone difference")
        void timezoneOffsetInternational() {
            // On 2024-03-15: LHR = UTC+0, CDG = UTC+1
            // f1 arrives LHR at 20:00 local = 20:00 UTC
            // f2 departs LHR at 21:30 local = 21:30 UTC
            // layover = 90 min → should be accepted for international
            Flight f1 = makeFlight("JFK", "LHR",
                    "2024-03-15T08:00", "2024-03-15T20:00");
            Flight f2 = makeFlight("LHR", "CDG",
                    "2024-03-15T21:30", "2024-03-15T23:00");

            assertTrue(validator.isValidConnection(f1, f2),
                    "90-min layover at LHR should be accepted for international");
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private Airport makeAirport(String code, String country, String timezone) {
        Airport a = new Airport();
        a.setCode(code);
        a.setName(code + " Airport");
        a.setCity(code);
        a.setCountry(country);
        a.setTimezone(timezone);
        return a;
    }

    private Flight makeFlight(String origin, String destination,
                              String departure, String arrival) {
        Flight f = new Flight();
        f.setFlightNumber("TEST");
        f.setAirline("Test Airlines");
        f.setOrigin(origin);
        f.setDestination(destination);
        f.setDepartureTime(LocalDateTime.parse(departure));
        f.setArrivalTime(LocalDateTime.parse(arrival));
        f.setPrice(100.0);
        f.setAircraft("A320");
        return f;
    }
}
