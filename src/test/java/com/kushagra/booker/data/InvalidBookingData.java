package com.kushagra.booker.data;

import com.kushagra.booker.models.Booking;
import java.util.Map;

/** Test-only invalid payloads; valid defaults remain in the reusable data factory. */
public final class InvalidBookingData {
    private InvalidBookingData() { }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> withoutField(Booking booking, String field) {
        Map<String, Object> payload = BookingData.asMap(booking);
        if (field.startsWith("bookingdates.")) {
            ((Map<String, Object>) payload.get("bookingdates")).remove(field.substring("bookingdates.".length()));
        } else {
            payload.remove(field);
        }
        return payload;
    }
}
