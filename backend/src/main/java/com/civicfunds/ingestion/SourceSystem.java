package com.civicfunds.ingestion;

/**
 * Every source CivicFunds ingests, with the provenance a citation needs.
 *
 * <p>DEMO is always rendered with a visible DEMO badge in the UI and is never
 * presented as actual government funding.
 */
public enum SourceSystem {

    USASPENDING("USAspending.gov", "https://api.usaspending.gov/", true),
    FDOT_CONTRACTS("FDOT All Construction Contracts", "https://gis.fdot.gov/arcgis/rest/services/All_Construction_Contracts/MapServer/0", true),
    FDOT_WORK_PROGRAM("FDOT Work Program", "https://gis.fdot.gov/arcgis/rest/services/Work_Program_Current/MapServer", true),
    RESILIENT_FLORIDA("Resilient Florida Grant Program (FDEP)", "https://services1.arcgis.com/nRHtyn3uE1kyzoYc/arcgis/rest/services/Resilient_Florida_Grant_Program___All_Grants/FeatureServer/64", true),
    COJ_LEGISTAR("Jacksonville City Council (Legistar)", "https://webapi.legistar.com/v1/jaxcityc/matters", true),
    NFTPO_TIP("North Florida TPO Transportation Improvement Program", "https://northfloridatpo.com/planning/tip", false),
    COJ_CIP("City of Jacksonville Capital Improvement Plan", "https://www.jacksonville.gov/budget", false),
    DEMO("Demonstration data - NOT actual government funding", "", false);

    private final String displayName;
    private final String baseUrl;
    private final boolean machineReadable;

    SourceSystem(String displayName, String baseUrl, boolean machineReadable) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.machineReadable = machineReadable;
    }

    public String displayName() {
        return displayName;
    }

    public String baseUrl() {
        return baseUrl;
    }

    /**
     * False for sources extracted from published PDFs. Those records are
     * semi-automated and manually verified, and the UI says so.
     */
    public boolean isMachineReadable() {
        return machineReadable;
    }

    public boolean isDemo() {
        return this == DEMO;
    }
}
