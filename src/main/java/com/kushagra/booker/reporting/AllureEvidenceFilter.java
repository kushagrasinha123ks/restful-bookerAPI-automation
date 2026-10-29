package com.kushagra.booker.reporting;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/** Records sanitized evidence using the runtime API; no AspectJ agent is needed. */
public final class AllureEvidenceFilter implements Filter {
    private final EvidenceSanitizer sanitizer;

    public AllureEvidenceFilter(EvidenceSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec, FilterContext context) {
        StringBuilder evidence = new StringBuilder(request.getMethod()).append(' ')
                .append(sanitizer.sanitize(request.getURI())).append('\n');
        request.getHeaders().forEach(header -> evidence.append(header.getName()).append(": ")
                .append(sanitizer.sensitiveHeader(header.getName()) ? "[REDACTED]" : sanitizer.sanitize(header.getValue()))
                .append('\n'));
        Object body = request.getBody();
        evidence.append('\n').append(sanitizer.sanitize(body == null ? null : body.toString()));
        if (!request.getFormParams().isEmpty()) {
            evidence.append("\nForm: ").append(sanitizer.sanitize(request.getFormParams().toString()));
        }
        Allure.addAttachment("HTTP request", "text/plain", evidence.toString(), "txt");
        long start = System.nanoTime();
        try {
            Response response = context.next(request, responseSpec);
            StringBuilder received = new StringBuilder(response.getStatusLine())
                    .append("\nElapsed: ").append((System.nanoTime() - start) / 1_000_000).append(" ms\n");
            response.getHeaders().forEach(header -> received.append(header.getName()).append(": ")
                    .append(sanitizer.sensitiveHeader(header.getName()) ? "[REDACTED]" : sanitizer.sanitize(header.getValue()))
                    .append('\n'));
            received.append('\n').append(sanitizer.sanitize(response.asString()));
            Allure.addAttachment("HTTP response", "text/plain", received.toString(), "txt");
            return response;
        } catch (Exception exception) {
            Allure.addAttachment("Transport failure", "text/plain",
                    exception.getClass().getSimpleName() + ": " + sanitizer.sanitize(exception.getMessage()), "txt");
            // Groovy transport can propagate checked I/O exceptions through the filter interface.
            throw new IllegalStateException("HTTP transport failed: " + exception.getClass().getSimpleName(), exception);
        }
    }
}
