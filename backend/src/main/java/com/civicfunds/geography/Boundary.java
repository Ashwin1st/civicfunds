package com.civicfunds.geography;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A council district, ZIP, neighbourhood or county polygon.
 *
 * <p>The geometry column is intentionally NOT mapped here. Point-in-polygon
 * assignment runs as a native PostGIS query; adding Hibernate Spatial to map
 * a column this application never reads through JPA would be a dependency
 * with no payoff.
 */
@Entity
@Table(name = "boundary")
@Getter
@Setter
@NoArgsConstructor
public class Boundary {

    public enum BoundaryType {
        COUNCIL_DISTRICT, CONGRESSIONAL_DISTRICT, ZIP, NEIGHBORHOOD, COUNTY, CITY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "boundary_type", nullable = false, length = 32)
    private BoundaryType boundaryType;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "retrieved_at")
    private Instant retrievedAt;
}
