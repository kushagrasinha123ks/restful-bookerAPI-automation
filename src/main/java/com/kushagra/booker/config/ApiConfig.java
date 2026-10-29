package com.kushagra.booker.config;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

/** Immutable configuration. System properties take precedence over environment variables. */
public record ApiConfig(String baseUrl, String username, String password,
                        int connectTimeoutMs, int readTimeoutMs) {
    public ApiConfig {
        URI uri = URI.create(baseUrl);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("baseUrl must be an HTTP(S) URL without credentials, query or fragment");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("API credentials must not be empty");
        }
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("HTTP timeouts must be positive milliseconds");
        }
    }

    public static ApiConfig load() {
        return from(System.getProperties(), System.getenv());
    }

    public static ApiConfig from(Properties properties, Map<String, String> environment) {
        return new ApiConfig(
                value(properties, environment, "baseUrl", "BOOKER_BASE_URL", "https://restful-booker.herokuapp.com"),
                value(properties, environment, "apiUsername", "BOOKER_USERNAME", "admin"),
                value(properties, environment, "apiPassword", "BOOKER_PASSWORD", "password123"),
                timeout(properties, environment, "connectTimeoutMs", "BOOKER_CONNECT_TIMEOUT_MS", "10000"),
                timeout(properties, environment, "readTimeoutMs", "BOOKER_READ_TIMEOUT_MS", "20000"));
    }

    private static String value(Properties properties, Map<String, String> environment,
                                String property, String variable, String fallback) {
        String result = properties.getProperty(property);
        if (result == null) result = environment.getOrDefault(variable, fallback);
        return result;
    }

    private static int timeout(Properties properties, Map<String, String> environment,
                               String property, String variable, String fallback) {
        try {
            return Integer.parseInt(value(properties, environment, property, variable, fallback));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(property + " must be an integer number of milliseconds");
        }
    }

    @Override
    public String toString() {
        return "ApiConfig[baseUrl=" + baseUrl + ", credentials=[REDACTED], connectTimeoutMs="
                + connectTimeoutMs + ", readTimeoutMs=" + readTimeoutMs + "]";
    }
}
