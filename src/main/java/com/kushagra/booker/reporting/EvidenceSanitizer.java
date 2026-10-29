package com.kushagra.booker.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Redacts credentials from report evidence without modifying the HTTP request or response. */
public final class EvidenceSanitizer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> SENSITIVE = List.of("password", "token", "authorization", "cookie", "username");
    private final List<String> secretValues;

    public EvidenceSanitizer(String... secrets) {
        secretValues = java.util.Arrays.stream(secrets)
                .filter(value -> value != null && !value.isEmpty()).toList();
    }

    public boolean sensitiveHeader(String name) {
        return SENSITIVE.stream().anyMatch(key -> name.toLowerCase(Locale.ROOT).contains(key));
    }

    public String sanitize(String text) {
        if (text == null || text.isEmpty()) return "<empty>";
        String result = text;
        try {
            JsonNode document = JSON.readTree(text);
            if (document != null) {
                redact(document);
                result = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(document);
            }
        } catch (java.io.IOException ignored) {
            // XML, form bodies and malformed JSON still get literal credential redaction below.
        }
        for (String secret : secretValues) result = result.replace(secret, "[REDACTED]");
        return result;
    }

    private void redact(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<String> names = object.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (sensitiveHeader(name)) object.put(name, "[REDACTED]");
                else redact(object.get(name));
            }
        } else if (node.isArray()) {
            node.forEach(this::redact);
        }
    }
}
