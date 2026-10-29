package com.kushagra.booker.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kushagra.booker.models.Booking;
import com.kushagra.booker.models.BookingDates;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BookingData {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> GENERATED_MARKERS = ConcurrentHashMap.newKeySet();
    private BookingData() { }

    public static Booking valid() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        GENERATED_MARKERS.add(unique);
        LocalDate checkin = LocalDate.now(ZoneOffset.UTC).plusDays(7);
        return Booking.builder().firstname("QA" + unique).lastname("Automation" + unique)
                .totalprice(321).depositpaid(true)
                .bookingdates(new BookingDates(checkin.toString(), checkin.plusDays(3).toString()))
                .additionalneeds("Breakfast").build();
    }

    /** Deep, writable map so individual invalid fields can be varied independently. */
    public static Map<String, Object> asMap(Booking booking) {
        return JSON.convertValue(booking, new TypeReference<>() { });
    }

    /** A reset can recycle IDs: only delete a record still carrying this run's unique marker. */
    public static boolean isGeneratedByThisRun(String firstname, String lastname) {
        return GENERATED_MARKERS.stream().anyMatch(marker ->
                (firstname != null && firstname.contains(marker))
                        || (lastname != null && lastname.contains(marker)));
    }
}
