package com.skypath.backend.controller;

import com.skypath.backend.model.Itinerary;
import com.skypath.backend.service.FlightSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin
public class FlightSearchController {

    private final FlightSearchService service;

    public FlightSearchController(FlightSearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public List<Itinerary> search(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date) {

        return service.search(origin, destination, date);
    }
}