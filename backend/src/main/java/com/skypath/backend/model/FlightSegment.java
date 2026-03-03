package com.skypath.backend.model;

public class FlightSegment {
    private Flight flight;
    private long layoverMinutes;

    public FlightSegment(Flight flight, long layoverMinutes) {
        this.flight = flight;
        this.layoverMinutes = layoverMinutes;
    }

    public Flight getFlight() { return flight; }
    public long getLayoverMinutes() { return layoverMinutes; }
}