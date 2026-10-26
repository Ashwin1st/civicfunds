package com.civicfunds.organization;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A name a source system actually used. The original string is always kept -
 * normalization never overwrites what the government published.
 */
@Entity
@Table(name = "organization_alias")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 300)
    private String alias;

    @Column(name = "normalized_alias", nullable = false, length = 300)
    private String normalizedAlias;

    @Column(name = "source_system", length = 64)
    private String sourceSystem;

    public OrganizationAlias(String alias, String normalizedAlias, String sourceSystem) {
        this.alias = alias;
        this.normalizedAlias = normalizedAlias;
        this.sourceSystem = sourceSystem;
    }
}
