package com.kushagra.booker.tests;

import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.data.InvalidBookingData;
import com.kushagra.booker.models.Booking;
import com.kushagra.booker.support.BaseApiTest;
import com.kushagra.booker.support.BookingAssertions;
import io.qameta.allure.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.Map;

@Epic("Restful Booker")
@Feature("Replace bookings")
@Test(groups = "regression")
public class BookingUpdateTest extends BaseApiTest {
    @Test(groups = "smoke")
    public void tokenAllowsFullUpdateAndReadBack() {
        int id = createBooking();
        Booking replacement = BookingData.valid();
        BookingAssertions.assertBooking(bookings.update(id, replacement, token()), replacement);
        BookingAssertions.assertBooking(bookings.get(id), replacement);
    }

    @Test
    public void basicAuthenticationAllowsFullUpdate() {
        int id = createBooking();
        Booking replacement = BookingData.valid();
        BookingAssertions.assertBooking(bookings.updateWithBasicAuth(id, replacement), replacement);
        BookingAssertions.assertBooking(bookings.get(id), replacement);
    }

    @Test(groups = "negative")
    public void updateWithoutAuthenticationIsRejectedAndDoesNotMutateData() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        bookings.updateUnauthenticated(id, BookingData.valid()).then().statusCode(403);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @Test(groups = "negative")
    public void updateWithInvalidTokenIsRejectedAndDoesNotMutateData() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        bookings.update(id, BookingData.valid(), "invalid-test-token").then().statusCode(403);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @DataProvider(name = "missingUpdateFields")
    public Object[][] missingUpdateFields() {
        return new Object[][] {{"firstname"}, {"lastname"}, {"totalprice"}, {"depositpaid"},
                {"bookingdates"}, {"bookingdates.checkin"}, {"bookingdates.checkout"}};
    }

    @Test(dataProvider = "missingUpdateFields", groups = "negative")
    public void incompleteReplacementIsRejectedAndOriginalSurvives(String field) {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        Map<String, Object> payload = InvalidBookingData.withoutField(BookingData.valid(), field);
        bookings.update(id, payload, token()).then().statusCode(400);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @Test(groups = "negative")
    @Description("Existing service returns 405 for a missing booking on write operations.")
    public void updatingDeletedBookingReturnsMethodNotAllowed() {
        int id = createBooking();
        bookings.delete(id, token()).then().statusCode(201);
        forget(id);
        bookings.update(id, BookingData.valid(), token()).then().statusCode(405);
        bookings.get(id).then().statusCode(404);
    }
}
