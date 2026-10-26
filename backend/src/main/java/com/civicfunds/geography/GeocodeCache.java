package com.civicfunds.geography;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * USAspending returns no coordinates, so records must be geocoded. The
 * pipeline reruns often; each unique address should reach the geocoder once.
 */
@Entity
@Table(name = "geocode_cache")
@Getter
@Setter
@NoArgsConstructor
public class GeocodeCache {

    @Id
    @Column(name = "normalized_address", length = 500)
    private String normalizedAddress;

    private Double latitude;
    private Double longitude;

    @Column(name = "match_quality", length = 32)
    private String matchQuality;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt = Instant.now();
}
