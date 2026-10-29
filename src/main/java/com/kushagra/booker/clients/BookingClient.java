package com.kushagra.booker.clients;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

/** HTTP transport only. Expected results and assertions belong to tests. */
public final class BookingClient {
    private final RequestFactory requests;

    public BookingClient(RequestFactory requests) { this.requests = requests; }

    public Response list() { return list(Map.of()); }
    public Response list(Map<String, ?> filters) {
        return requests.request().accept("application/json").queryParams(filters).get("/booking");
    }
    public Response get(int id) { return get(Integer.toString(id)); }
    public Response get(String id) {
        return requests.request().accept("application/json").pathParam("id", id).get("/booking/{id}");
    }
    public Response get(int id, String accept) {
        return requests.request().accept(accept).pathParam("id", id).get("/booking/{id}");
    }
    public Response create(Object payload) { return create(payload, "application/json", "application/json"); }
    public Response create(Object payload, String contentType, String accept) {
        RequestSpecification request = requests.request().contentType(contentType).accept(accept);
        if ("application/x-www-form-urlencoded".equals(contentType) && payload instanceof Map<?, ?> map) {
            map.forEach((key, value) -> request.formParam(key.toString(), value));
        } else {
            request.body(payload);
        }
        return request.post("/booking");
    }
    public Response update(int id, Object payload, String token) {
        return write(requests.request().cookie("token", token), "PUT", id, payload);
    }
    public Response updateWithBasicAuth(int id, Object payload) {
        return write(requests.basicAuthRequest(), "PUT", id, payload);
    }
    public Response updateUnauthenticated(int id, Object payload) {
        return write(requests.request(), "PUT", id, payload);
    }
    public Response patch(int id, Object payload, String token) {
        return write(requests.request().cookie("token", token), "PATCH", id, payload);
    }
    public Response patchWithBasicAuth(int id, Object payload) {
        return write(requests.basicAuthRequest(), "PATCH", id, payload);
    }
    public Response patchUnauthenticated(int id, Object payload) {
        return write(requests.request(), "PATCH", id, payload);
    }
    public Response delete(int id, String token) { return delete(requests.request().cookie("token", token), id); }
    public Response deleteWithBasicAuth(int id) { return delete(requests.basicAuthRequest(), id); }
    public Response deleteUnauthenticated(int id) { return delete(requests.request(), id); }

    private Response write(RequestSpecification request, String method, int id, Object payload) {
        return request.contentType("application/json").accept("application/json")
                .pathParam("id", id).body(payload).request(method, "/booking/{id}");
    }
    private Response delete(RequestSpecification request, int id) {
        return request.pathParam("id", id).delete("/booking/{id}");
    }
}
