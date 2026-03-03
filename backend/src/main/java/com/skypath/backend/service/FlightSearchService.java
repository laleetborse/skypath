package com.skypath.backend.service;

import com.skypath.backend.exception.AirportNotFoundException;
import com.skypath.backend.exception.FlightSearchException;
import com.skypath.backend.model.*;
import com.skypath.backend.repository.FlightDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class FlightSearchService {

    private final FlightDataRepository repository;
    private final ConnectionValidator validator;
    private final TimeService timeService;

    public FlightSearchService(FlightDataRepository repository,
                               ConnectionValidator validator,
                               TimeService timeService) {
        this.repository = repository;
        this.validator = validator;
        this.timeService = timeService;
    }

    public List<Itinerary> search(String origin,
                                  String destination,
                                  String date) {

        if (!repository.getAirports().containsKey(origin))
            throw new AirportNotFoundException(origin);
        if (!repository.getAirports().containsKey(destination))
            throw new AirportNotFoundException(destination);

        if (origin.equals(destination))
            return Collections.emptyList();

        LocalDate departureDate;
        try {
            departureDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new FlightSearchException("Invalid date format. Expected yyyy-MM-dd, got: " + date);
        }

        List<Itinerary> results = new ArrayList<>();
        Queue<List<Flight>> queue = new LinkedList<>();

        List<Flight> firstFlights =
                repository.getFlightsByOrigin()
                        .getOrDefault(origin, List.of());

        for (Flight flight : firstFlights) {
            if (flight.getDepartureTime().toLocalDate().equals(departureDate)) {
                queue.add(List.of(flight));
            }
        }

        while (!queue.isEmpty()) {

            List<Flight> path = queue.poll();
            Flight last = path.get(path.size() - 1);

            if (path.size() > 3) continue;

            if (last.getDestination().equals(destination)) {
                results.add(buildItinerary(path));
                continue;
            }

            List<Flight> nextFlights =
                    repository.getFlightsByOrigin()
                            .getOrDefault(last.getDestination(), List.of());

            for (Flight next : nextFlights) {
                if (validator.isValidConnection(last, next)) {
                    List<Flight> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        results.sort(Comparator.comparingLong(Itinerary::getTotalDurationMinutes));

        return results;
    }

    private Itinerary buildItinerary(List<Flight> flights) {

        List<FlightSegment> segments = new ArrayList<>();
        double totalPrice = 0;

        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);
            totalPrice += flight.getPrice();
            segments.add(new FlightSegment(flight, 0));
        }

        // Duration calculation
        Flight first = flights.get(0);
        Flight last = flights.get(flights.size() - 1);

        Airport originAirport =
                repository.getAirports().get(first.getOrigin());
        Airport destAirport =
                repository.getAirports().get(last.getDestination());

        ZonedDateTime start =
                timeService.toUTC(first.getDepartureTime(), originAirport);

        ZonedDateTime end =
                timeService.toUTC(last.getArrivalTime(), destAirport);

        long totalDuration =
                timeService.minutesBetween(start, end);

        return new Itinerary(segments, totalDuration, totalPrice);
    }
}