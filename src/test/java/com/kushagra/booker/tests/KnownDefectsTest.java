package com.kushagra.booker.tests;

import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.models.Booking;
import com.kushagra.booker.models.BookingDates;
import com.kushagra.booker.support.BaseApiTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

/** Strict expectations: intentionally fail while the documented defects remain unresolved. */
@Epic("Restful Booker")
@Feature("Known defects: strict expectations")
@Test(groups = "known-defect")
public class KnownDefectsTest extends BaseApiTest {
    @Test(groups = "known-defect")
    @Issue("OBS-001")
    @Description("Quality expectation: missing required input should be rejected as a client error. Current POST implementation returns 500.")
    public void missingRequiredInputShouldReturnBadRequest() {
        Map<String, Object> payload = BookingData.asMap(BookingData.valid());
        payload.remove("firstname");
        Response response = bookings.create(payload);
        track(response);
        response.then().statusCode(400);
    }

    @Test(groups = "known-defect")
    @Issue("OBS-002")
    @Description("Generated API documentation specifies inclusive checkin filtering. Current deployment excludes a booking whose checkin equals the query boundary.")
    public void checkinFilterShouldIncludeEqualBoundary() {
        Booking sample = BookingData.valid();
        Booking expected = new Booking(sample.firstname(), sample.lastname(), sample.totalprice(),
                sample.depositpaid(), new BookingDates("2030-06-10", "2030-06-15"), sample.additionalneeds());
        int id = createBooking(expected);
        bookings.list(Map.of("firstname", expected.firstname(), "checkin", "2030-06-10"))
                .then().statusCode(200).body("bookingid", hasItem(id));
    }

    @Test(groups = "known-defect")
    @Issue("OBS-006")
    @Description("Generated API documentation defines checkout as an inclusive lower bound. A booking checking out after the query date should match, but the deployment currently filters in the opposite direction.")
    public void checkoutFilterShouldTreatQueryAsInclusiveLowerBound() {
        Booking sample = BookingData.valid();
        Booking expected = new Booking(sample.firstname(), sample.lastname(), sample.totalprice(),
                sample.depositpaid(), new BookingDates("2030-06-10", "2030-06-15"), sample.additionalneeds());
        int id = createBooking(expected);
        bookings.list(Map.of("firstname", expected.firstname(), "checkout", "2030-06-14"))
                .then().statusCode(200).body("bookingid", hasItem(id));
    }

    @Test(groups = "known-defect")
    @Issue("OBS-003")
    @Description("Patching one nested date should preserve the omitted checkout date. Current implementation replaces the nested object and corrupts the sibling date.")
    public void nestedDatePatchShouldPreserveCheckout() {
        Booking original = BookingData.valid();
        int id = createBooking(original);
        bookings.patch(id, Map.of("bookingdates", Map.of("checkin", "2031-01-10")), token())
                .then().statusCode(200).body("bookingdates.checkout", equalTo(original.bookingdates().checkout()));
    }

    @Test(groups = "known-defect")
    @Issue("OBS-004")
    @Description("XML responses should be labelled application/xml. Current service returns an XML body with text/html content type.")
    public void xmlResponseShouldHaveXmlContentType() {
        int id = createBooking();
        bookings.get(id, "application/xml").then().statusCode(200).contentType("application/xml");
    }

    @Test(groups = "known-defect")
    @Issue("OBS-005")
    @Description("The form-encoded boolean false should persist as false. Current JavaScript Boolean conversion treats a non-empty 'false' string as true.")
    public void formEncodedFalseShouldRemainFalse() {
        Booking sample = BookingData.valid();
        String form = "firstname=" + sample.firstname() + "&lastname=" + sample.lastname()
                + "&totalprice=75&depositpaid=false&bookingdates%5Bcheckin%5D=2030-01-10"
                + "&bookingdates%5Bcheckout%5D=2030-01-12&additionalneeds=Breakfast";
        Response created = bookings.create(form, "application/x-www-form-urlencoded", "application/json");
        track(created);
        created.then().statusCode(200).body("booking.depositpaid", equalTo(false));
    }
}
