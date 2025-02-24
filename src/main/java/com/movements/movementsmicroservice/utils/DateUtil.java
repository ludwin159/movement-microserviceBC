package com.movements.movementsmicroservice.utils;

import java.time.*;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class DateUtil {
    private static final Map<Boolean, Function<LocalDate, LocalDateTime>> DATE_TIME_MAPPER = Map.of(
            true, LocalDate::atStartOfDay,
            false, date -> date.atTime(23, 59, 59)
    );

    public static LocalDateTime toUtc(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(dt -> DateUtil.toLocalDateTime(localDateTime.toInstant(ZoneOffset.UTC)))
                .orElseThrow(() -> new IllegalArgumentException("Date is null or not permit"));
    }

    public static LocalDateTime parseDateStringToUtc(String dateStr, boolean isStart) {
        return Optional.ofNullable(dateStr)
                .map(LocalDate::parse)
                .map(DATE_TIME_MAPPER.get(isStart))
                .map(DateUtil::toUtc)
                .orElseThrow(() -> new IllegalArgumentException("Invalid date: " + dateStr));
    }

    public static LocalDateTime parseToUtc(Object input, boolean isStart) {
        return Optional.ofNullable(input)
                .map(obj -> {
                    if (obj instanceof String) {
                        return parseDateStringToUtc((String) obj, isStart);
                    }
                    if (obj instanceof LocalDateTime) {
                        return toUtc((LocalDateTime) obj);
                    }
                    throw new IllegalArgumentException("Type of date is not supported: " + obj.getClass());
                })
                .orElseThrow(() -> new IllegalArgumentException("Date is null or not permit"));
    }
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return Optional.of(instant)
                        .map(date -> LocalDateTime.ofInstant(date, ZoneOffset.UTC))
                .orElseThrow(() -> new IllegalArgumentException("Invalid date: " + instant));
    }

}

