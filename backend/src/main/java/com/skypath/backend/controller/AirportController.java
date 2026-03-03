package com.skypath.backend.controller;

import com.skypath.backend.model.Airport;
import com.skypath.backend.repository.FlightDataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/airports")
@CrossOrigin
public class AirportController {

    private final FlightDataRepository repository;

    public AirportController(FlightDataRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Collection<Airport> getAll() {
        return repository.getAirports().values();
    }

    @GetMapping("/search")
    public List<Airport> search(@RequestParam String q) {
        String query = q.trim().toLowerCase();
        return repository.getAirports().values().stream()
                .filter(a ->
                        a.getCode().toLowerCase().contains(query) ||
                        a.getName().toLowerCase().contains(query) ||
                        a.getCity().toLowerCase().contains(query) ||
                        a.getCountry().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }
}
