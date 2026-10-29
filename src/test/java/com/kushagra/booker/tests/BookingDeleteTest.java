package com.kushagra.booker.tests;

import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.models.Booking;
import com.kushagra.booker.support.BaseApiTest;
import com.kushagra.booker.support.BookingAssertions;
import io.qameta.allure.*;
import org.testng.annotations.Test;

@Epic("Restful Booker")
@Feature("Delete bookings")
@Test(groups = "regression")
public class BookingDeleteTest extends BaseApiTest {
    @Test(groups = "smoke")
    public void tokenAllowsDeletionAndBookingBecomesUnavailable() {
        int id = createBooking();
        bookings.delete(id, token()).then().statusCode(201);
        forget(id);
        bookings.get(id).then().statusCode(404);
    }

    @Test
    public void basicAuthenticationAllowsDeletion() {
        int id = createBooking();
        bookings.deleteWithBasicAuth(id).then().statusCode(201);
        forget(id);
        bookings.get(id).then().statusCode(404);
    }

    @Test(groups = "negative")
    public void deleteWithoutAuthenticationIsRejectedAndBookingRemains() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        bookings.deleteUnauthenticated(id).then().statusCode(403);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @Test(groups = "negative")
    public void deleteWithInvalidTokenIsRejectedAndBookingRemains() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        bookings.delete(id, "invalid-test-token").then().statusCode(403);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @Test(groups = "negative")
    @Description("Repeated DELETE returns 405 in this application; GET confirms the resource remains absent.")
    public void repeatedDeletionReturnsMethodNotAllowed() {
        int id = createBooking();
        bookings.delete(id, token()).then().statusCode(201);
        forget(id);
        bookings.delete(id, token()).then().statusCode(405);
        bookings.get(id).then().statusCode(404);
    }
}
