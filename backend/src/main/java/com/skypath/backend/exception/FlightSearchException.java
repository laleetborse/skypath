package com.skypath.backend.exception;

public class FlightSearchException extends RuntimeException {

    public FlightSearchException(String message) {
        super(message);
    }

    public FlightSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
