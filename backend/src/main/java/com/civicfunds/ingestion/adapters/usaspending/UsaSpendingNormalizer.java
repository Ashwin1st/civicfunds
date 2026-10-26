package com.civicfunds.ingestion.adapters.usaspending;

import com.civicfunds.common.GovernmentLevel;
import com.civicfunds.common.money.AmountClass;
import com.civicfunds.common.text.Normalizer;
import com.civicfunds.ingestion.RawFundingRecord;
import com.civicfunds.ingestion.SourceSystem;
import com.civicfunds.normalization.NormalizedFundingRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Turns a stored USAspending row into CivicFunds' own vocabulary.
 *
 * <h2>The amount-class mapping, which is the whole point</h2>
 *
 * USAspending's {@code Award Amount} corresponds to the award's
 * {@code total_obligation} - money the government has committed, not an award
 * ceiling and not cash paid. It is therefore mapped to
 * {@link AmountClass#OBLIGATED}, and {@code Total Outlays} to
 * {@link AmountClass#OUTLAYED} as the secondary figure.
 *
 * <p><b>Verify this against live data before trusting a chart.</b> If
 * {@code Award Amount} turns out to track {@code base_and_all_options} for some
 * contract types, that subset is {@link AmountClass#AWARDED} instead. Getting
 * this wrong does not crash anything - it quietly mislabels every federal
 * figure in the application, which is worse.
 *
 * <p>Outlay reporting was optional for agencies before FY2022, so
 * {@code Total Outlays} on older awards is incomplete rather than zero. It is
 * stored as published; the UI is responsible for the caveat.
 *
 * <p>Loans are a different quantity again - a face value, not a grant - so they
 * are mapped to {@link AmountClass#ESTIMATED} and never summed with anything.
 */
@Component
public class UsaSpendingNormalizer {

    private final ObjectMapper objectMapper;

    public UsaSpendingNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedFundingRecord normalize(RawFundingRecord raw) {
        JsonNode row;
        try {
            row = objectMapper.readTree(raw.getRawPayload());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Raw record " + raw.getId() + " does not contain readable JSON", e);
        }

        NormalizedFundingRecord out = new NormalizedFundingRecord();
        out.setRawRecord(raw);
        out.setSourceSystem(SourceSystem.USASPENDING.name());
        out.setGovernmentLevel(GovernmentLevel.FEDERAL);
        out.setSourceUrl(raw.getSourceUrl());
        out.setRetrievedAt(raw.getRetrievedAt());

        out.setAwardId(text(row, "Award ID"));
        out.setAwardType(firstNonNull(text(row, "Award Type"), text(row, "Contract Award Type")));

        boolean isLoan = row.hasNonNull("Loan Value");
        if (isLoan) {
            out.setAmount(decimal(row, "Loan Value"));
            out.setAmountClass(AmountClass.ESTIMATED);
            if (row.hasNonNull("Subsidy Cost")) {
                out.setSecondaryAmount(decimal(row, "Subsidy Cost"));
                out.setSecondaryAmountClass(AmountClass.ESTIMATED);
            }
        } else {
            BigDecimal amount = decimal(row, "Award Amount");
            out.setAmount(amount);
            out.setAmountClass(amount == null ? AmountClass.UNKNOWN : AmountClass.OBLIGATED);

            BigDecimal outlays = decimal(row, "Total Outlays");
            if (outlays != null) {
                out.setSecondaryAmount(outlays);
                out.setSecondaryAmountClass(AmountClass.OUTLAYED);
            }
        }

        out.setRecipientRaw(text(row, "Recipient Name"));
        out.setFundingAgencyRaw(firstNonNull(
                text(row, "Funding Agency"), text(row, "Awarding Agency")));

        // USAspending has no project title field. The description is the only
        // project-level text there is, and its own documentation warns it is
        // often an administrative code rather than words. Both are kept: the
        // description as the description, and a trimmed form as a weak title.
        String description = text(row, "Description");
        out.setProjectDescription(description);
        out.setProjectTitle(weakTitle(description));

        // The assistance listing (CFDA) is the most reliable programme-level
        // identity available, and the best available join key when a funding
        // opportunity number is absent.
        String cfda = text(row, "CFDA Number");
        if (cfda != null) {
            out.setExternalKey("CFDA:" + cfda);
        }

        out.setStartDate(date(row, "Start Date"));
        out.setEndDate(date(row, "End Date"));
        out.setAwardDate(firstNonNull(date(row, "Base Obligation Date"), date(row, "Issued Date")));

        JsonNode pop = row.path("Primary Place of Performance");
        if (pop.isObject()) {
            out.setCity(Normalizer.trimToNull(pop.path("city_name").asText(null)));
            out.setCounty(Normalizer.trimToNull(pop.path("county_name").asText(null)));
            out.setState(Normalizer.trimToNull(pop.path("state_code").asText(null)));
            out.setZip5(Normalizer.trimToNull(pop.path("zip5").asText(null)));
            out.setCongressionalDistrict(
                    Normalizer.trimToNull(pop.path("congressional_code").asText(null)));
        }
        // Latitude and longitude are never returned by this API. Geocoding runs
        // as a later stage against the city/ZIP recorded above.

        return out;
    }

    /**
     * A usable title from a description, or null. Never invents one: if the
     * description is an administrative code or empty, the project stays
     * untitled and matching falls back to location and identifiers.
     */
    static String weakTitle(String description) {
        String value = Normalizer.trimToNull(description);
        if (value == null) return null;
        // Pure administrative code. The colon and slash matter: contract rows
        // carry values like "IGF::OT::IGF", which is a procurement classification,
        // not a description of anything built.
        if (value.matches("[A-Z0-9._:/\\-]{2,}")) return null;
        if (value.length() <= 120) return value;
        int cut = value.lastIndexOf(' ', 120);
        return value.substring(0, cut > 40 ? cut : 120).trim();
    }

    private static String text(JsonNode row, String field) {
        JsonNode node = row.path(field);
        return node.isMissingNode() || node.isNull()
                ? null : Normalizer.trimToNull(node.asText());
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        JsonNode node = row.path(field);
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate date(JsonNode row, String field) {
        String value = text(row, field);
        if (value == null) return null;
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (Exception e) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }
}
