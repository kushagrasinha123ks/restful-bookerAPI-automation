package com.kushagra.booker.tests;

import com.kushagra.booker.config.ApiConfig;
import com.kushagra.booker.clients.RequestFactory;
import com.kushagra.booker.data.BookingData;
import com.kushagra.booker.reporting.EvidenceSanitizer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.Map;
import java.util.Properties;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/** Offline checks for configuration and evidence safety; these do not replace API coverage. */
@Epic("Framework")
@Feature("Configuration, evidence redaction and data ownership")
@Test(groups = "framework")
public class FrameworkTest {
    @Test
    public void configurationUsesPublicDemoDefaults() {
        ApiConfig config = ApiConfig.from(new Properties(), Map.of());
        assertEquals(config.baseUrl(), "https://restful-booker.herokuapp.com");
        assertEquals(config.connectTimeoutMs(), 10000);
        assertEquals(config.readTimeoutMs(), 20000);
    }

    @Test
    public void propertiesOverrideEnvironmentWithoutLeakingCredentials() {
        Properties properties = new Properties();
        properties.setProperty("baseUrl", "http://localhost:3001/");
        properties.setProperty("apiPassword", "private-test-password");
        properties.setProperty("readTimeoutMs", "12500");
        ApiConfig config = ApiConfig.from(properties,
                Map.of("BOOKER_BASE_URL", "http://localhost:4000", "BOOKER_PASSWORD", "environment-secret"));
        assertEquals(config.baseUrl(), "http://localhost:3001");
        assertEquals(config.password(), "private-test-password");
        assertEquals(config.readTimeoutMs(), 12500);
        assertFalse(config.toString().contains(config.password()));
    }

    @DataProvider(name = "invalidBaseUrls")
    public Object[][] invalidBaseUrls() {
        return new Object[][] {{"file:///tmp/booker"}, {"https://user:secret@example.com"},
                {"https://example.com?token=secret"}, {"https://example.com#fragment"}, {"relative/path"}};
    }

    @Test(dataProvider = "invalidBaseUrls", expectedExceptions = IllegalArgumentException.class)
    public void configurationRejectsUnsafeOrInvalidBaseUrls(String url) {
        new ApiConfig(url, "user", "secret", 1000, 1000);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void zeroTimeoutIsRejectedInsteadOfWaitingIndefinitely() {
        new ApiConfig("http://localhost:3001", "user", "secret", 0, 1000);
    }

    @Test
    public void jsonEvidenceRedactsNestedTokensAndPasswords() {
        String input = "{\"password\":\"secret\",\"items\":[{\"token\":\"runtime-token\"}],\"firstname\":\"QA\"}";
        String output = new EvidenceSanitizer().sanitize(input);
        assertFalse(output.contains("secret"));
        assertFalse(output.contains("runtime-token"));
        assertTrue(output.contains("QA"));
    }

    @Test
    public void malformedAndXmlEvidenceStillRedactsConfiguredCredentials() {
        EvidenceSanitizer sanitizer = new EvidenceSanitizer("sensitive-value");
        assertFalse(sanitizer.sanitize("<password>sensitive-value</password>").contains("sensitive-value"));
        assertFalse(sanitizer.sanitize("{\"password\":\"sensitive-value").contains("sensitive-value"));
        assertTrue(sanitizer.sensitiveHeader("Authorization"));
        assertTrue(sanitizer.sensitiveHeader("Set-Cookie"));
    }

    @Test
    public void ownershipMarkersRejectUnrelatedRecordsAndSurviveNamePrefixChanges() {
        var booking = BookingData.valid();
        assertTrue(BookingData.isGeneratedByThisRun(booking.firstname(), booking.lastname()));
        assertTrue(BookingData.isGeneratedByThisRun("Changed-" + booking.firstname(), null));
        assertFalse(BookingData.isGeneratedByThisRun("Jim", "Brown"));
        assertFalse(BookingData.isGeneratedByThisRun(null, null));
    }

    @Test
    public void transportFailureIsWrappedAndHttpRequestsAreNotRetried() throws java.io.IOException {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/disconnect", exchange -> { calls.incrementAndGet(); exchange.close(); });
        server.start();
        try {
            var config = new ApiConfig("http://127.0.0.1:" + server.getAddress().getPort(), "user", "secret", 1000, 1000);
            var exception = expectThrows(IllegalStateException.class,
                    () -> new RequestFactory(config).request().get("/disconnect"));
            assertTrue(exception.getMessage().startsWith("HTTP transport failed:"));
            assertEquals(calls.get(), 1, "Transport must not silently retry a request");
        } finally {
            server.stop(0);
        }
    }
}
