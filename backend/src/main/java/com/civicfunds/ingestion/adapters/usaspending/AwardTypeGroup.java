package com.civicfunds.ingestion.adapters.usaspending;

import java.util.List;

/**
 * USAspending validates that every requested award type belongs to ONE of its
 * groups, and that the requested fields are valid for that group. Mixing types
 * across groups is rejected with HTTP 422.
 *
 * <p>These groupings are not ours to choose. They are reproduced from the
 * {@code award_type_groups} block the API itself returns in that 422 response,
 * which is the authoritative listing: the public documentation names the types
 * but not which group each one belongs to.
 *
 * <p>Learned the hard way. An earlier version collapsed grants, other financial
 * assistance and direct payments into one {@code NON_LOAN_ASSISTANCE} group. It
 * passed every unit test — the tests assert on the request we build, not on
 * whether the server accepts it — and went unnoticed in practice because a
 * record cap was being spent entirely on contracts before the ingestion ever
 * reached that group.
 */
public enum AwardTypeGroup {

    CONTRACTS(List.of("A", "B", "C", "D"),
            List.of("Start Date", "End Date", "Award Amount", "Total Outlays",
                    "Contract Award Type", "NAICS", "PSC")),

    IDV(List.of("IDV_A", "IDV_B", "IDV_B_A", "IDV_B_B", "IDV_B_C", "IDV_C", "IDV_D", "IDV_E"),
            List.of("Start Date", "Award Amount", "Total Outlays",
                    "Contract Award Type", "Last Date to Order", "NAICS", "PSC")),

    /**
     * Grants carry a CFDA (Assistance Listing) number, which is the only
     * deterministic federal identifier in this dataset. Contracts have none, so
     * an ingestion without grants gives identifier matching nothing to match.
     */
    GRANTS(List.of("02", "03", "04", "05", "F001", "F002"),
            List.of("Start Date", "End Date", "Award Amount", "Total Outlays",
                    "Award Type", "SAI Number", "CFDA Number", "Assistance Listings")),

    LOANS(List.of("07", "08", "F003", "F004"),
            List.of("Issued Date", "Loan Value", "Subsidy Cost", "SAI Number",
                    "CFDA Number", "Assistance Listings")),

    OTHER_FINANCIAL_ASSISTANCE(List.of("06", "10", "F006", "F007"),
            List.of("Start Date", "End Date", "Award Amount", "Total Outlays",
                    "Award Type", "SAI Number", "CFDA Number", "Assistance Listings")),

    DIRECT_PAYMENTS(List.of("09", "F005", "11", "-1", "F008", "F009", "F010"),
            List.of("Start Date", "End Date", "Award Amount", "Total Outlays",
                    "Award Type", "SAI Number", "CFDA Number", "Assistance Listings"));

    /** Requested for every group. */
    public static final List<String> BASE_FIELDS = List.of(
            "Award ID", "Recipient Name", "Recipient UEI", "recipient_id",
            "Awarding Agency", "Awarding Sub Agency",
            "Funding Agency", "Funding Sub Agency",
            "Description", "Last Modified Date", "Base Obligation Date",
            "generated_internal_id", "def_codes",
            // Object-valued. Carries county_code / county_name / city_name /
            // congressional_code, which the flat fields do not.
            "Primary Place of Performance", "Recipient Location");

    private final List<String> codes;
    private final List<String> extraFields;

    AwardTypeGroup(List<String> codes, List<String> extraFields) {
        this.codes = codes;
        this.extraFields = extraFields;
    }

    public List<String> codes() {
        return codes;
    }

    public List<String> fields() {
        return java.util.stream.Stream.concat(BASE_FIELDS.stream(), extraFields.stream()).toList();
    }

    /** The field to sort on; must be one of the requested fields. */
    public String sortField() {
        return this == LOANS ? "Loan Value" : "Award Amount";
    }
}
