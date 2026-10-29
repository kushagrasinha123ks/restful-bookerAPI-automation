package com.kushagra.booker.tests;

import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.data.InvalidBookingData;
import com.kushagra.booker.models.Booking;
import com.kushagra.booker.models.BookingDates;
import com.kushagra.booker.support.BaseApiTest;
import com.kushagra.booker.support.BookingAssertions;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@Epic("Restful Booker")
@Feature("Create bookings")
@Test(groups = "regression")
public class BookingCreateTest extends BaseApiTest {
    @DataProvider(name = "validBookings")
    public Object[][] validBookings() {
        return new Object[][] {
            {"standard booking", 125, true, "Breakfast"},
            {"zero price boundary", 0, false, "Breakfast"},
            {"minimum positive price", 1, false, ""},
            {"large integer price", 2147483647, true, "Late checkout"}
        };
    }

    @Test(dataProvider = "validBookings", groups = {"smoke", "contract"})
    public void validBookingPersistsEveryField(String scenario, int price, boolean paid, String needs) {
        Booking sample = BookingData.valid();
        Booking expected = new Booking(sample.firstname(), sample.lastname(), price, paid,
                sample.bookingdates(), needs);
        Response created = bookings.create(expected);
        track(created);
        BookingAssertions.assertCreated(created, expected);
        BookingAssertions.assertBooking(bookings.get(created.jsonPath().getInt("bookingid")), expected);
    }

    @Test
    @Description("Although generated docs mark additionalneeds required, current service accepts it as optional.")
    public void bookingWithoutAdditionalNeedsIsAccepted() {
        Booking sample = BookingData.valid();
        Map<String, Object> payload = BookingData.asMap(sample);
        payload.remove("additionalneeds");
        Response created = bookings.create(payload);
        track(created);
        created.then().statusCode(200).body("booking.additionalneeds", nullValue());
        bookings.get(created.jsonPath().getInt("bookingid")).then().statusCode(200)
                .body("firstname", equalTo(sample.firstname())).body("additionalneeds", nullValue());
    }

    @DataProvider(name = "missingRequiredFields")
    public Object[][] missingRequiredFields() {
        return new Object[][] {{"firstname"}, {"lastname"}, {"totalprice"}, {"depositpaid"},
                {"bookingdates"}, {"bookingdates.checkin"}, {"bookingdates.checkout"}};
    }

    @Test(dataProvider = "missingRequiredFields", groups = "negative")
    @Issue("OBS-001")
    @Description("Characterization: missing required fields currently return 500. This does not endorse a server error as correct validation; strict 400 checks run in the known-defect suite.")
    public void incompleteBookingCurrentlyReturnsServerError(String field) {
        Map<String, Object> payload = InvalidBookingData.withoutField(BookingData.valid(), field);
        Response response = bookings.create(payload);
        track(response);
        response.then().statusCode(500);
    }

    @Test(groups = "negative")
    public void malformedBookingJsonReturnsBadRequest() {
        bookings.create("{\"firstname\":").then().statusCode(400);
    }

    @Test(groups = "contract")
    public void xmlRequestCanCreateBooking() {
        Booking sample = BookingData.valid();
        Booking expected = new Booking(sample.firstname(), sample.lastname(), 75, true,
                new BookingDates("2030-01-10", "2030-01-12"), "Breakfast");
        String xml = "<booking><firstname>" + expected.firstname() + "</firstname><lastname>"
                + expected.lastname() + "</lastname><totalprice>75</totalprice><depositpaid>true</depositpaid>"
                + "<bookingdates><checkin>2030-01-10</checkin><checkout>2030-01-12</checkout></bookingdates>"
                + "<additionalneeds>Breakfast</additionalneeds></booking>";
        Response created = bookings.create(xml, "text/xml", "application/json");
        track(created);
        BookingAssertions.assertCreated(created, expected);
        BookingAssertions.assertBooking(bookings.get(created.jsonPath().getInt("bookingid")), expected);
    }

    @Test(groups = "contract")
    public void formEncodedRequestCanCreateBooking() {
        Booking sample = BookingData.valid();
        Booking expected = new Booking(sample.firstname(), sample.lastname(), 75, true,
                new BookingDates("2030-01-10", "2030-01-12"), "Breakfast");
        String form = "firstname=" + expected.firstname() + "&lastname=" + expected.lastname()
                + "&totalprice=75&depositpaid=true&bookingdates%5Bcheckin%5D=2030-01-10"
                + "&bookingdates%5Bcheckout%5D=2030-01-12&additionalneeds=Breakfast";
        Response created = bookings.create(form, "application/x-www-form-urlencoded", "application/json");
        track(created);
        BookingAssertions.assertCreated(created, expected);
        BookingAssertions.assertBooking(bookings.get(created.jsonPath().getInt("bookingid")), expected);
    }
}
