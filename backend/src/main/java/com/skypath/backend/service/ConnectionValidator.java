package com.skypath.backend.service;

import com.skypath.backend.model.Airport;
import com.skypath.backend.model.Flight;
import com.skypath.backend.repository.FlightDataRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class ConnectionValidator {

    private final TimeService timeService;
    private final FlightDataRepository repository;

    public ConnectionValidator(TimeService timeService,
                               FlightDataRepository repository) {
        this.timeService = timeService;
        this.repository = repository;
    }

    public boolean isValidConnection(Flight prev, Flight next) {

        if (!prev.getDestination().equals(next.getOrigin()))
            return false;

        Airport arrivalAirport = repository.getAirports()
                .get(prev.getDestination());

        Airport departureAirport = repository.getAirports()
                .get(next.getOrigin());

        ZonedDateTime arrivalUTC =
                timeService.toUTC(prev.getArrivalTime(), arrivalAirport);

        ZonedDateTime departureUTC =
                timeService.toUTC(next.getDepartureTime(), departureAirport);

        long layover = timeService.minutesBetween(arrivalUTC, departureUTC);

        if (layover < 0) return false;

        String prevOriginCountry = repository.getAirports()
                .get(prev.getOrigin()).getCountry();
        String connectionCountry = arrivalAirport.getCountry();
        String nextDestCountry = repository.getAirports()
                .get(next.getDestination()).getCountry();

        boolean domestic = prevOriginCountry.equals(connectionCountry)
                && connectionCountry.equals(nextDestCountry);

        long minLayover = domestic ? 45 : 90;

        return layover >= minLayover && layover <= 360;
    }
}