package com.kushagra.booker.support;

import com.kushagra.booker.models.Booking;
import com.kushagra.booker.models.CreateBookingResponse;
import io.restassured.response.Response;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class BookingAssertions {
    private BookingAssertions() { }

    public static void assertBooking(Response response, Booking expected) {
        response.then().statusCode(200).contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"));
        assertEquals(response.as(Booking.class), expected, "Returned booking must match every expected field");
    }

    public static void assertCreated(Response response, Booking expected) {
        response.then().statusCode(200).contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schemas/create-booking.json"));
        CreateBookingResponse created = response.as(CreateBookingResponse.class);
        assertTrue(created.bookingid() > 0, "Generated booking ID must be positive");
        assertEquals(created.booking(), expected, "Created booking must match the submitted fields");
    }
}
