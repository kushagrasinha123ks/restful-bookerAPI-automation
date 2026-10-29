package com.kushagra.booker.tests;

import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.models.Booking;
import com.kushagra.booker.models.BookingDates;
import com.kushagra.booker.support.BaseApiTest;
import com.kushagra.booker.support.BookingAssertions;
import io.qameta.allure.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.Map;

@Epic("Restful Booker")
@Feature("Partially update bookings")
@Test(groups = "regression")
public class BookingPatchTest extends BaseApiTest {
    @DataProvider(name = "partialFields")
    public Object[][] partialFields() {
        return new Object[][] {{"firstname"}, {"lastname"}, {"totalprice"}, {"depositpaid"},
                {"bookingdates"}, {"additionalneeds"}};
    }

    @Test(dataProvider = "partialFields", groups = "smoke")
    public void patchChangesSelectedFieldAndPreservesOtherFields(String field) {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        Object value = switch (field) {
            case "firstname" -> "Changed-" + original.firstname();
            case "lastname" -> "Changed-" + original.lastname();
            case "totalprice" -> 0;
            case "depositpaid" -> !original.depositpaid();
            case "bookingdates" -> Map.of("checkin", "2031-07-01", "checkout", "2031-07-03");
            default -> "Airport transfer";
        };
        Booking expected = new Booking(
                field.equals("firstname") ? (String) value : original.firstname(),
                field.equals("lastname") ? (String) value : original.lastname(),
                field.equals("totalprice") ? (Integer) value : original.totalprice(),
                field.equals("depositpaid") ? (Boolean) value : original.depositpaid(),
                field.equals("bookingdates") ? new BookingDates("2031-07-01", "2031-07-03") : original.bookingdates(),
                field.equals("additionalneeds") ? (String) value : original.additionalneeds());
        BookingAssertions.assertBooking(bookings.patch(id, Map.of(field, value), token()), expected);
        BookingAssertions.assertBooking(bookings.get(id), expected);
    }

    @Test
    public void basicAuthenticationAllowsPartialUpdate() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        Booking expected = new Booking(original.firstname(), original.lastname(), 456,
                original.depositpaid(), original.bookingdates(), original.additionalneeds());
        BookingAssertions.assertBooking(bookings.patchWithBasicAuth(id, Map.of("totalprice", 456)), expected);
        BookingAssertions.assertBooking(bookings.get(id), expected);
    }

    @Test
    public void emptyPatchKeepsBookingUnchanged() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        BookingAssertions.assertBooking(bookings.patch(id, Map.of(), token()), original);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @Test(groups = "negative")
    public void patchWithoutAuthenticationIsRejectedWithoutChangingBooking() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        bookings.patchUnauthenticated(id, Map.of("firstname", "Unauthorized")).then().statusCode(403);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @Test(groups = "negative")
    public void patchWithInvalidTokenIsRejectedWithoutChangingBooking() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        bookings.patch(id, Map.of("firstname", "Unauthorized"), "invalid-test-token").then().statusCode(403);
        BookingAssertions.assertBooking(bookings.get(id), original);
    }

    @Test(groups = "negative")
    public void patchingDeletedBookingReturnsMethodNotAllowed() {
        int id = createBooking();
        bookings.delete(id, token()).then().statusCode(201);
        forget(id);
        bookings.patch(id, Map.of("firstname", "Missing"), token()).then().statusCode(405);
        bookings.get(id).then().statusCode(404);
    }
}
