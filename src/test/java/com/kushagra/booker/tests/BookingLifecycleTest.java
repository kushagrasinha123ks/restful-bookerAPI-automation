package com.kushagra.booker.tests;

import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.models.Booking;
import com.kushagra.booker.support.BaseApiTest;
import com.kushagra.booker.support.BookingAssertions;
import io.qameta.allure.*;
import org.testng.annotations.Test;
import java.util.Map;
import static io.qameta.allure.Allure.step;
import static org.hamcrest.Matchers.hasItem;

@Epic("Restful Booker")
@Feature("Booking lifecycle")
public class BookingLifecycleTest extends BaseApiTest {
    @Test(groups = {"regression", "smoke", "lifecycle"})
    @Story("Create, find, read, replace, patch and delete one owned booking")
    public void completeBookingLifecycle() {
        Booking initial = BookingData.valid();
        int id = step("Create a unique booking", () -> createBooking(initial));
        step("Find the new booking by guest name", () ->
                bookings.list(Map.of("firstname", initial.firstname())).then().statusCode(200).body("bookingid", hasItem(id)));
        step("Read back the created data", () -> BookingAssertions.assertBooking(bookings.get(id), initial));
        Booking replacement = BookingData.valid();
        step("Replace the booking and verify persistence", () -> {
            BookingAssertions.assertBooking(bookings.update(id, replacement, token()), replacement);
            BookingAssertions.assertBooking(bookings.get(id), replacement);
        });
        Booking patched = new Booking(replacement.firstname(), replacement.lastname(), replacement.totalprice(),
                replacement.depositpaid(), replacement.bookingdates(), "Late checkout");
        step("Patch additional needs while preserving other fields", () -> {
            BookingAssertions.assertBooking(bookings.patch(id, Map.of("additionalneeds", "Late checkout"), token()), patched);
            BookingAssertions.assertBooking(bookings.get(id), patched);
        });
        step("Delete and confirm the booking no longer exists", () -> {
            bookings.delete(id, token()).then().statusCode(201);
            forget(id);
            bookings.get(id).then().statusCode(404);
        });
    }
}
