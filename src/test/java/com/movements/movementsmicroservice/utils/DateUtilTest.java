package com.movements.movementsmicroservice.utils;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class DateUtilTest {

    @Test
    void toUtc_ShouldConvertToUtc() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 2, 23, 12, 0);
        LocalDateTime result = DateUtil.toUtc(localDateTime);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 2, 23, 12, 0));
    }

    @Test
    void toUtc_ShouldThrowExceptionWhenNull() {
        assertThatThrownBy(() -> DateUtil.toUtc(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date is null or not permit");
    }

    @Test
    void parseDateStringToUtc_ShouldReturnStartOfDayUtc() {
        String dateStr = "2024-02-23";
        LocalDateTime result = DateUtil.parseDateStringToUtc(dateStr, true);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 2, 23, 0, 0));
    }

    @Test
    void parseDateStringToUtc_ShouldReturnEndOfDayUtc() {
        String dateStr = "2024-02-23";
        LocalDateTime result = DateUtil.parseDateStringToUtc(dateStr, false);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 2, 23, 23, 59, 59));
    }

    @Test
    void parseDateStringToUtc_ShouldThrowExceptionForInvalidDate() {
        assertThatThrownBy(() -> DateUtil.parseDateStringToUtc("invalid-date", true))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    void parseToUtc_ShouldHandleStringInput() {
        String dateStr = "2024-02-23";
        LocalDateTime result = DateUtil.parseToUtc(dateStr, true);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 2, 23, 0, 0));
    }

    @Test
    void parseToUtc_ShouldHandleLocalDateTimeInput() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 2, 23, 15, 30);
        LocalDateTime result = DateUtil.parseToUtc(dateTime, true);
        assertThat(result).isEqualTo(dateTime);
    }

    @Test
    void parseToUtc_ShouldThrowExceptionForUnsupportedType() {
        assertThatThrownBy(() -> DateUtil.parseToUtc(12345, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Type of date is not supported");
    }

    @Test
    void toLocalDateTime_ShouldConvertInstantToUtc() {
        Instant instant = Instant.parse("2024-02-23T12:00:00Z");
        LocalDateTime result = DateUtil.toLocalDateTime(instant);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 2, 23, 12, 0));
    }

    @Test
    void toLocalDateTime_ShouldThrowExceptionForNullInstant() {
        assertThatThrownBy(() -> DateUtil.toLocalDateTime(null))
                .isInstanceOf(NullPointerException.class);
    }
}
