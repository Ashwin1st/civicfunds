package com.civicfunds.common.text;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("same content in a different key order produces the same checksum")
    void keyOrderDoesNotChangeChecksum() throws Exception {
        var a = mapper.readTree("{\"b\":2,\"a\":1,\"c\":{\"y\":2,\"x\":1}}");
        var b = mapper.readTree("{\"a\":1,\"c\":{\"x\":1,\"y\":2},\"b\":2}");

        assertThat(CanonicalJson.checksum(mapper, a))
                .isEqualTo(CanonicalJson.checksum(mapper, b));
    }

    @Test
    @DisplayName("a changed value produces a different checksum")
    void changedValueChangesChecksum() throws Exception {
        var a = mapper.readTree("{\"amount\":100}");
        var b = mapper.readTree("{\"amount\":101}");

        assertThat(CanonicalJson.checksum(mapper, a))
                .isNotEqualTo(CanonicalJson.checksum(mapper, b));
    }

    @Test
    @DisplayName("array order is preserved, because in these payloads it means something")
    void arrayOrderMatters() throws Exception {
        var a = mapper.readTree("{\"listings\":[\"20.205\",\"20.507\"]}");
        var b = mapper.readTree("{\"listings\":[\"20.507\",\"20.205\"]}");

        assertThat(CanonicalJson.checksum(mapper, a))
                .isNotEqualTo(CanonicalJson.checksum(mapper, b));
    }

    @Test
    @DisplayName("checksum is 64 lowercase hex characters")
    void checksumShape() {
        assertThat(CanonicalJson.sha256("civicfunds"))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }
}
