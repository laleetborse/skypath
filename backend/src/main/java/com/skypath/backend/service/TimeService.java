package com.skypath.backend.service;

import com.skypath.backend.model.Airport;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
public class TimeService {

    public ZonedDateTime toUTC(LocalDateTime localTime, Airport airport) {
        return localTime
                .atZone(ZoneId.of(airport.getTimezone()))
                .withZoneSameInstant(ZoneOffset.UTC);
    }

    public long minutesBetween(ZonedDateTime start, ZonedDateTime end) {
        return Duration.between(start, end).toMinutes();
    }
}