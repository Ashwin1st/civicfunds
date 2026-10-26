package com.civicfunds.common.text;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Turns a JSON payload into one canonical string so that the same record always
 * produces the same checksum.
 *
 * <p>This matters more than it looks. Government APIs do not guarantee key
 * order, and a re-serialisation with shuffled keys would look like a changed
 * record and defeat idempotent ingestion. Sorting object keys recursively fixes
 * that. Array order is preserved, because in these payloads array order is
 * meaningful (transaction sequences, assistance listings).
 */
public final class CanonicalJson {

    private CanonicalJson() { }

    public static String canonicalise(ObjectMapper mapper, JsonNode node) {
        try {
            return mapper.writeValueAsString(sort(mapper, node));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not canonicalise JSON payload", e);
        }
    }

    private static JsonNode sort(ObjectMapper mapper, JsonNode node) {
        if (node == null || node.isNull()) {
            return mapper.nullNode();
        }
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            ObjectNode out = mapper.createObjectNode();
            for (String name : names) {
                out.set(name, sort(mapper, node.get(name)));
            }
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = mapper.createArrayNode();
            node.forEach(child -> out.add(sort(mapper, child)));
            return out;
        }
        return node;
    }

    /** SHA-256 of the canonical form, lowercase hex. 64 characters. */
    public static String checksum(ObjectMapper mapper, JsonNode node) {
        return sha256(canonicalise(mapper, node));
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
