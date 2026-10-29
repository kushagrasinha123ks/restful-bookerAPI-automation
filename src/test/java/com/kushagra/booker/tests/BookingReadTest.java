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
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;

@Epic("Restful Booker")
@Feature("Read bookings and filters")
@Test(groups = "regression")
public class BookingReadTest extends BaseApiTest {
    @Test(groups = {"smoke", "contract"})
    public void bookingListContainsOnlyValidIdentifierObjects() {
        int ownId = createBooking();
        bookings.list().then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking-ids.json"))
                .body("bookingid", hasItem(ownId));
    }

    @Test(groups = {"smoke", "contract"})
    public void createdBookingCanBeReadBack() {
        Booking expected = BookingData.valid();
        int id = createBooking(expected);
        BookingAssertions.assertBooking(bookings.get(id), expected);
    }

    @Test(groups = "negative")
    public void deletedIdentifierReturnsNotFound() {
        int id = createBooking();
        bookings.delete(id, token()).then().statusCode(201);
        forget(id);
        bookings.get(id).then().statusCode(404);
    }

    @DataProvider(name = "invalidIdentifiers")
    public Object[][] invalidIdentifiers() {
        return new Object[][] {{"not-a-number"}, {"0"}, {"-1"}};
    }

    @Test(dataProvider = "invalidIdentifiers", groups = "negative")
    public void invalidIdentifierReturnsNotFound(String id) {
        bookings.get(id).then().statusCode(404);
    }

    @DataProvider(name = "nameFilters")
    public Object[][] nameFilters() {
        return new Object[][] {{"firstname"}, {"lastname"}, {"both"}};
    }

    @Test(dataProvider = "nameFilters")
    public void namesFilterCreatedBooking(String filter) {
        Booking expected = BookingData.valid();
        int id = createBooking(expected);
        Map<String, String> query = switch (filter) {
            case "firstname" -> Map.of("firstname", expected.firstname());
            case "lastname" -> Map.of("lastname", expected.lastname());
            default -> Map.of("firstname", expected.firstname(), "lastname", expected.lastname());
        };
        bookings.list(query).then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking-ids.json"))
                .body("bookingid", contains(id));
    }

    @Test(groups = "negative")
    public void unmatchedNameReturnsEmptyList() {
        bookings.list(Map.of("firstname", "NoMatch-" + java.util.UUID.randomUUID()))
                .then().statusCode(200).body("$", empty());
    }

    @Test
    public void dateRangeIncludesOwnedBookingInsideBounds() {
        Booking sample = BookingData.valid();
        Booking expected = new Booking(sample.firstname(), sample.lastname(), sample.totalprice(),
                sample.depositpaid(), new BookingDates("2030-06-10", "2030-06-15"), sample.additionalneeds());
        int id = createBooking(expected);
        bookings.list(Map.of("firstname", expected.firstname(), "checkin", "2030-06-09", "checkout", "2030-06-16"))
                .then().statusCode(200).body("bookingid", contains(id));
    }

    @Test(groups = "negative")
    public void dateRangeExcludesOwnedBookingOutsideBounds() {
        Booking sample = BookingData.valid();
        Booking expected = new Booking(sample.firstname(), sample.lastname(), sample.totalprice(),
                sample.depositpaid(), new BookingDates("2030-06-10", "2030-06-15"), sample.additionalneeds());
        createBooking(expected);
        bookings.list(Map.of("firstname", expected.firstname(), "checkin", "2030-06-11"))
                .then().statusCode(200).body("$", empty());
    }

    @Test(groups = "contract")
    @Description("XML body validation is independent of its incorrect text/html response header; strict header expectation is in KnownDefectsTest.")
    public void bookingCanBeReadAsXml() {
        Booking expected = BookingData.valid();
        int id = createBooking(expected);
        var response = bookings.get(id, "application/xml");
        response.then().statusCode(200);
        assertEquals(response.xmlPath().getString("booking.firstname"), expected.firstname());
        assertEquals(response.xmlPath().getString("booking.lastname"), expected.lastname());
        assertEquals(response.xmlPath().getString("booking.bookingdates.checkin"), expected.bookingdates().checkin());
    }

    @Test(groups = "negative")
    @Description("Characterization of the service's unsupported Accept response: HTTP 418.")
    public void unsupportedResponseFormatReturnsTeapot() {
        int id = createBooking();
        bookings.get(id, "application/pdf").then().statusCode(418);
    }
}
