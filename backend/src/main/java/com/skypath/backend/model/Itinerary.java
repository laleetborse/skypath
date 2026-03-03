package com.skypath.backend.model;

import java.util.List;

public class Itinerary {
    private List<FlightSegment> segments;
    private long totalDurationMinutes;
    private double totalPrice;

    public Itinerary(List<FlightSegment> segments,
                     long totalDurationMinutes,
                     double totalPrice) {
        this.segments = segments;
        this.totalDurationMinutes = totalDurationMinutes;
        this.totalPrice = totalPrice;
    }

    public List<FlightSegment> getSegments() { return segments; }
    public long getTotalDurationMinutes() { return totalDurationMinutes; }
    public double getTotalPrice() { return totalPrice; }
}