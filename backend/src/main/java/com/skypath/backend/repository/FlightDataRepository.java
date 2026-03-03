package com.skypath.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypath.backend.model.Airport;
import com.skypath.backend.model.Flight;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class FlightDataRepository {

    private Map<String, Airport> airports = new HashMap<>();
    private Map<String, List<Flight>> flightsByOrigin = new HashMap<>();

    public Map<String, Airport> getAirports() {
        return airports;
    }

    public Map<String, List<Flight>> getFlightsByOrigin() {
        return flightsByOrigin;
    }

    @PostConstruct
    public void loadData() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("/flights.json");

        Map<String, Object> data = mapper.readValue(is, Map.class);

        List<Map<String, Object>> airportList = (List<Map<String, Object>>) data.get("airports");
        for (Map<String, Object> a : airportList) {
            Airport airport = mapper.convertValue(a, Airport.class);
            airports.put(airport.getCode(), airport);
        }

        List<Map<String, Object>> flightList = (List<Map<String, Object>>) data.get("flights");
        for (Map<String, Object> f : flightList) {
            Flight flight = new Flight();
            flight.setFlightNumber((String) f.get("flightNumber"));
            flight.setAirline((String) f.get("airline"));
            flight.setOrigin((String) f.get("origin"));
            flight.setDestination((String) f.get("destination"));
            flight.setDepartureTime(LocalDateTime.parse((String) f.get("departureTime")));
            flight.setArrivalTime(LocalDateTime.parse((String) f.get("arrivalTime")));
            Object priceObj = f.get("price");
            double price;
            if (priceObj instanceof Number) {
                price = ((Number) priceObj).doubleValue();
            } else if (priceObj instanceof String) {
                price = Double.parseDouble((String) priceObj);
            } else {
                throw new IllegalArgumentException("Unexpected price value: " + priceObj);
            }
            flight.setPrice(price);
            flight.setAircraft((String) f.get("aircraft"));

            flightsByOrigin
                .computeIfAbsent(flight.getOrigin(), k -> new ArrayList<>())
                .add(flight);
        }
    }
}