package com.kushagra.booker.clients;

import io.restassured.response.Response;

public final class HealthClient {
    private final RequestFactory requests;

    public HealthClient(RequestFactory requests) { this.requests = requests; }

    public Response ping() { return requests.request().get("/ping"); }

    public Response postPing() { return requests.request().post("/ping"); }
}
