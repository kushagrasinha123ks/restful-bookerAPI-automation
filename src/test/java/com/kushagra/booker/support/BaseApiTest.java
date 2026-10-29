package com.kushagra.booker.support;

import com.kushagra.booker.clients.AuthClient;
import com.kushagra.booker.clients.BookingClient;
import com.kushagra.booker.clients.HealthClient;
import com.kushagra.booker.clients.RequestFactory;
import com.kushagra.booker.config.ApiConfig;
import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.models.Booking;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/** Per-test ownership and cleanup; suite execution is deliberately sequential for the public demo. */
public abstract class BaseApiTest {
    protected ApiConfig config;
    protected RequestFactory requests;
    protected AuthClient auth;
    protected BookingClient bookings;
    protected HealthClient health;
    private final Set<Integer> ownedIds = new LinkedHashSet<>();
    private String currentToken;

    @BeforeClass(alwaysRun = true)
    public void configureClients() {
        config = ApiConfig.load();
        requests = new RequestFactory(config);
        auth = new AuthClient(requests, config);
        bookings = new BookingClient(requests);
        health = new HealthClient(requests);
    }

    @BeforeMethod(alwaysRun = true)
    public void beginIndependentTest() {
        ownedIds.clear();
        currentToken = null;
    }

    protected String token() {
        if (currentToken == null) currentToken = Allure.step("Obtain authentication token", () -> auth.token());
        return currentToken;
    }

    protected int createBooking() { return createBooking(BookingData.valid()); }

    protected int createBooking(Booking booking) {
        return Allure.step("Create a booking owned by this test", () -> {
            Response response = bookings.create(booking);
            track(response);
            BookingAssertions.assertCreated(response, booking);
            return response.jsonPath().getInt("bookingid");
        });
    }

    protected void track(int id) {
        if (id > 0) ownedIds.add(id);
    }

    protected void track(Response response) {
        if (response == null || !response.contentType().contains("json")) return;
        try {
            Object value = response.jsonPath().get("bookingid");
            if (value instanceof Number id) track(id.intValue());
        } catch (RuntimeException ignored) {
            // A malformed/non-creation response must not hide the actual assertion outcome.
        }
    }

    protected void forget(int id) { ownedIds.remove(id); }

    @AfterMethod(alwaysRun = true)
    public void cleanupOwnedBookings(ITestResult result) {
        List<String> failures = new ArrayList<>();
        for (int id : List.copyOf(ownedIds)) {
            try {
                Allure.step("Clean up owned booking " + id, () -> {
                    Response current = bookings.get(id);
                    if (current.statusCode() == 404) return;
                    if (current.statusCode() != 200 || !BookingData.isGeneratedByThisRun(
                            current.jsonPath().getString("firstname"), current.jsonPath().getString("lastname"))) {
                        throw new IllegalStateException("Ownership changed after a reset or external modification; refusing deletion");
                    }
                    Response deleted = bookings.delete(id, token());
                    if (deleted.statusCode() != 201 && deleted.statusCode() != 405) {
                        throw new IllegalStateException("Cleanup returned HTTP " + deleted.statusCode());
                    }
                    if (bookings.get(id).statusCode() != 404) {
                        throw new IllegalStateException("Booking remains retrievable after cleanup");
                    }
                });
            } catch (Exception | AssertionError exception) {
                failures.add("Booking " + id + ": " + exception.getClass().getSimpleName());
            }
        }
        ownedIds.clear();
        if (!failures.isEmpty()) {
            String message = "Cleanup failed for owned data: " + String.join(", ", failures);
            Allure.addAttachment("Cleanup failures", message);
            if (result.isSuccess()) throw new AssertionError(message);
            // Preserve the original test failure, while keeping cleanup evidence visible.
        }
    }
}
