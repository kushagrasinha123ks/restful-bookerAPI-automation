package com.kushagra.booker.clients;

import com.kushagra.booker.config.ApiConfig;
import io.restassured.response.Response;
import java.util.Map;

public final class AuthClient {
    private final RequestFactory requests;
    private final ApiConfig config;

    public AuthClient(RequestFactory requests, ApiConfig config) {
        this.requests = requests;
        this.config = config;
    }

    public Response createToken() {
        return createToken(Map.of("username", config.username(), "password", config.password()));
    }

    public Response createToken(Object payload) {
        return requests.request().contentType("application/json").accept("application/json")
                .body(payload).post("/auth");
    }

    public String token() {
        Response response = createToken();
        if (response.statusCode() != 200) throw new IllegalStateException("Token creation returned HTTP " + response.statusCode());
        String token = response.jsonPath().getString("token");
        if (token == null || token.isBlank()) throw new IllegalStateException("Token creation did not return a token; check API credentials");
        return token;
    }
}
