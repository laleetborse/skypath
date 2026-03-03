package com.skypath.backend.exception;

public class AirportNotFoundException extends RuntimeException {

    private final String airportCode;

    public AirportNotFoundException(String airportCode) {
        super("Airport not found: " + airportCode);
        this.airportCode = airportCode;
    }

    public String getAirportCode() {
        return airportCode;
    }
}
