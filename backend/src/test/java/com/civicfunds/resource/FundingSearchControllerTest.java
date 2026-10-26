package com.civicfunds.resource;

import com.civicfunds.resource.dto.SearchHit;
import com.civicfunds.resource.dto.SearchResponse;
import com.civicfunds.search.FundingSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc: no Spring context, so this stays fast and does not
 * depend on security or datasource configuration.
 */
class FundingSearchControllerTest {

    private FundingSearchService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(FundingSearchService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FundingSearchController(service)).build();
    }

    private static SearchResponse oneHit() {
        SearchHit hit = new SearchHit(
                1L, "IDENTIFIER", "Exact lookup on a government identifier. No scoring involved.",
                "SR-115 (Lem Turner Rd) bridge replacement",
                "USASPENDING", "FEDERAL",
                "75160444.00", "OBLIGATED",
                "FDOT DISTRICT 2", "DEPARTMENT OF TRANSPORTATION",
                "4374372", "Jacksonville · Duval County · 32218", "8",
                "https://api.usaspending.gov/api/v2/awards/", "2026-09-08T00:00:00Z");
        return new SearchResponse("FM 4374372", 1, Map.of("IDENTIFIER", 1),
                List.of("OBLIGATED"), false,
                "These records share one measurement class (OBLIGATED), but CivicFunds still shows no total.",
                List.of(hit));
    }

    @Test
    @DisplayName("GET /api/funding/search returns hits with their match kind")
    void returnsHits() throws Exception {
        when(service.search(eq("FM 4374372"), any())).thenReturn(oneHit());

        mvc.perform(get("/api/funding/search").param("q", "FM 4374372"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.hits[0].matchKind").value("IDENTIFIER"))
                .andExpect(jsonPath("$.hits[0].externalKey").value("4374372"));
    }

    @Test
    @DisplayName("The response never shows a total, and says why")
    void neverShowsATotal() throws Exception {
        when(service.search(any(), any())).thenReturn(oneHit());

        mvc.perform(get("/api/funding/search").param("q", "anything"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalShown").value(false))
                .andExpect(jsonPath("$.whyNoTotal").isNotEmpty());
    }

    @Test
    @DisplayName("Every figure is returned with its measurement class beside it")
    void amountAlwaysCarriesItsClass() throws Exception {
        when(service.search(any(), any())).thenReturn(oneHit());

        mvc.perform(get("/api/funding/search").param("q", "bridge"))
                .andExpect(jsonPath("$.hits[0].amount").value("75160444.00"))
                .andExpect(jsonPath("$.hits[0].amountClass").value("OBLIGATED"));
    }

    @Test
    @DisplayName("A missing q is a 400, not an empty search")
    void missingQueryIsABadRequest() throws Exception {
        mvc.perform(get("/api/funding/search"))
                .andExpect(status().isBadRequest());
    }
}
