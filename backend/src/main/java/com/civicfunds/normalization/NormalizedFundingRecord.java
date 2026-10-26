package com.civicfunds.normalization;

import com.civicfunds.common.GovernmentLevel;
import com.civicfunds.common.money.Amount;
import com.civicfunds.common.money.AmountClass;
import com.civicfunds.ingestion.RawFundingRecord;
import com.civicfunds.organization.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * One funding record, expressed in CivicFunds' own vocabulary.
 *
 * <p>Always derived from exactly one {@link RawFundingRecord}, and always able
 * to point back at it. Note that {@code amountClass} is not nullable: this
 * schema cannot represent a dollar figure that does not say what it measures.
 */
@Entity
@Table(name = "normalized_funding_record")
@Getter
@Setter
@NoArgsConstructor
public class NormalizedFundingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_record_id", nullable = false, unique = true)
    private RawFundingRecord rawRecord;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "government_level", nullable = false, length = 16)
    private GovernmentLevel governmentLevel;

    @Column(name = "award_id", length = 255)
    private String awardId;

    /**
     * The source's own project key where one exists - an FDOT FM/FPID number,
     * a DEP grant agreement number, a federal-aid project number. This is what
     * makes deterministic cross-source matching possible; everything else is
     * probabilistic.
     */
    @Column(name = "external_key", length = 255)
    private String externalKey;

    @Column(name = "award_type", length = 120)
    private String awardType;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_class", nullable = false, length = 24)
    private AmountClass amountClass = AmountClass.UNKNOWN;

    @Column(name = "secondary_amount", precision = 18, scale = 2)
    private BigDecimal secondaryAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_amount_class", length = 24)
    private AmountClass secondaryAmountClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_agency_id")
    private Organization fundingAgency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Organization recipient;

    /** The agency string exactly as the source published it. Never overwritten. */
    @Column(name = "funding_agency_raw", length = 300)
    private String fundingAgencyRaw;

    /** The recipient string exactly as the source published it. Never overwritten. */
    @Column(name = "recipient_raw", length = 300)
    private String recipientRaw;

    @Column(name = "project_title", length = 500)
    private String projectTitle;

    @Column(name = "project_description", columnDefinition = "text")
    private String projectDescription;

    @Column(length = 64)
    private String category;

    @Column(length = 32)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "award_date")
    private LocalDate awardDate;

    @Column(name = "fiscal_year", length = 16)
    private String fiscalYear;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String county;

    @Column(length = 2)
    private String state;

    @Column(length = 5)
    private String zip5;

    private Double latitude;
    private Double longitude;

    @Column(name = "council_district", length = 8)
    private String councilDistrict;

    @Column(name = "congressional_district", length = 8)
    private String congressionalDistrict;

    @Column(name = "source_url", nullable = false, columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated = Instant.now();

    /** The typed amount. Empty when the source published no figure. */
    @Transient
    public Optional<Amount> typedAmount() {
        return amount == null ? Optional.empty() : Optional.of(Amount.of(amount, amountClass));
    }

    @Transient
    public Optional<Amount> typedSecondaryAmount() {
        return (secondaryAmount == null || secondaryAmountClass == null)
                ? Optional.empty()
                : Optional.of(Amount.of(secondaryAmount, secondaryAmountClass));
    }

    @Transient
    public boolean hasDeterministicKey() {
        return externalKey != null && !externalKey.isBlank();
    }
}
