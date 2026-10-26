package com.civicfunds.organization;

import com.civicfunds.common.GovernmentLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An agency or a recipient. One table, because the distinction is a role in a
 * funding record, not a property of the organization: the City of Jacksonville
 * is a recipient of federal grants and a funder of local contracts.
 */
@Entity
@Table(name = "organization")
@Getter
@Setter
@NoArgsConstructor
public class Organization {

    /** CANDIDATE organizations are never silently merged - they queue for review. */
    public enum ReviewStatus { CONFIRMED, CANDIDATE, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_name", nullable = false, length = 300)
    private String canonicalName;

    @Column(name = "normalized_name", nullable = false, length = 300)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "government_level", nullable = false, length = 16)
    private GovernmentLevel governmentLevel;

    @Column(length = 12)
    private String uei;

    @Column(length = 255)
    private String domain;

    @Column(length = 120)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(length = 5)
    private String zip5;

    /** True for entries hand-authored in organizations.yaml. */
    @Column(nullable = false)
    private boolean seeded;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 16)
    private ReviewStatus reviewStatus = ReviewStatus.CONFIRMED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrganizationAlias> aliases = new ArrayList<>();

    public void addAlias(OrganizationAlias alias) {
        alias.setOrganization(this);
        this.aliases.add(alias);
    }
}
