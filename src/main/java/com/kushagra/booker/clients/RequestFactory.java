package com.kushagra.booker.clients;

import com.kushagra.booker.config.ApiConfig;
import com.kushagra.booker.reporting.AllureEvidenceFilter;
import com.kushagra.booker.reporting.EvidenceSanitizer;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

/** Creates a fresh specification for every request. No mutable REST Assured globals. */
public final class RequestFactory {
    private final ApiConfig config;

    public RequestFactory(ApiConfig config) {
        this.config = config;
    }

    @SuppressWarnings("deprecation")
    public RequestSpecification request() {
        return io.restassured.RestAssured.given(new RequestSpecBuilder()
                .setBaseUri(config.baseUrl())
                .addHeader("Connection", "close")
                .setConfig(RestAssuredConfig.config()
                        .encoderConfig(EncoderConfig.encoderConfig()
                                .appendDefaultContentCharsetToContentTypeIfUndefined(false))
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .httpClientFactory(() -> {
                                    // REST Assured 5 uses Apache's legacy client API.
                                    DefaultHttpClient client = new DefaultHttpClient();
                                    client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
                                    return client;
                                })
                                .setParam("http.connection.timeout", config.connectTimeoutMs())
                                .setParam("http.socket.timeout", config.readTimeoutMs())))
                .addFilter(new AllureEvidenceFilter(new EvidenceSanitizer(config.username(), config.password())))
                .build());
    }

    public RequestSpecification basicAuthRequest() {
        return request().auth().preemptive().basic(config.username(), config.password());
    }
}
