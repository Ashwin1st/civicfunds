package com.civicfunds.search;

import com.civicfunds.common.GovernmentLevel;
import com.civicfunds.common.money.AmountClass;
import com.civicfunds.normalization.NormalizedFundingRecord;
import com.civicfunds.normalization.NormalizedFundingRecordRepository;
import com.civicfunds.resource.dto.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rung ordering is a claim about confidence, so it is tested as one.
 */
class FundingSearchServiceTest {

    private NormalizedFundingRecordRepository repo;
    private FundingSearchService service;

    @BeforeEach
    void setUp() {
        repo = mock(NormalizedFundingRecordRepository.class);
        service = new FundingSearchService(repo);
        when(repo.searchByIdentifier(anyString(), anyInt())).thenReturn(List.of());
        when(repo.searchFullText(anyString(), anyInt())).thenReturn(List.of());
        when(repo.searchFuzzy(anyString(), anyInt())).thenReturn(List.of());
    }

    private static NormalizedFundingRecord record(long id, String title,
                                                  AmountClass cls, String amount) {
        NormalizedFundingRecord r = new NormalizedFundingRecord();
        r.setId(id);
        r.setProjectTitle(title);
        r.setSourceSystem("USASPENDING");
        r.setGovernmentLevel(GovernmentLevel.FEDERAL);
        r.setAmountClass(cls);
        r.setAmount(amount == null ? null : new BigDecimal(amount));
        r.setSourceUrl("https://api.usaspending.gov/");
        r.setRetrievedAt(Instant.parse("2026-09-08T00:00:00Z"));
        return r;
    }

    @Test
    @DisplayName("An identifier query goes to the identifier rung and is labelled IDENTIFIER")
    void identifierRung() {
        when(repo.searchByIdentifier(eq("4374372"), anyInt()))
                .thenReturn(List.of(record(1L, "Trout River Bridge", AmountClass.OBLIGATED, "75160444")));

        SearchResponse res = service.search("FM 4374372", null);

        assertEquals(1, res.total());
        assertEquals("IDENTIFIER", res.hits().get(0).matchKind());
        assertEquals(1, res.byMatchKind().get("IDENTIFIER"));
    }

    @Test
    @DisplayName("A phrase skips the identifier rung entirely")
    void phraseSkipsIdentifierRung() {
        when(repo.searchFullText(eq("Lem Turner"), anyInt()))
                .thenReturn(List.of(record(2L, "SR-115 Lem Turner Rd", AmountClass.OBLIGATED, "100")));

        SearchResponse res = service.search("Lem Turner", null);

        verify(repo, never()).searchByIdentifier(anyString(), anyInt());
        assertEquals("KEYWORD", res.hits().get(0).matchKind());
    }

    @Test
    @DisplayName("Fuzzy only runs when the first two rungs found nothing")
    void fuzzyIsALastResort() {
        when(repo.searchFullText(anyString(), anyInt()))
                .thenReturn(List.of(record(3L, "Drainage", AmountClass.OBLIGATED, "1")));

        service.search("drainage", null);
        verify(repo, never()).searchFuzzy(anyString(), anyInt());

        when(repo.searchFullText(anyString(), anyInt())).thenReturn(List.of());
        service.search("drainaje", null);
        verify(repo).searchFuzzy(eq("drainaje"), anyInt());
    }

    @Test
    @DisplayName("Two measurement classes: no total, and the reason names them")
    void refusesToTotalAcrossClasses() {
        when(repo.searchFullText(anyString(), anyInt())).thenReturn(List.of(
                record(4L, "Bridge — obligated", AmountClass.OBLIGATED, "4100000"),
                record(5L, "Bridge — outlayed", AmountClass.OUTLAYED, "900000")));

        SearchResponse res = service.search("bridge", null);

        assertFalse(res.totalShown());
        assertEquals(2, res.amountClassesPresent().size());
        assertTrue(res.whyNoTotal().contains("OBLIGATED"));
        assertTrue(res.whyNoTotal().contains("OUTLAYED"));
    }

    @Test
    @DisplayName("One class is still no total — same class can still be the same dollars")
    void refusesToTotalWithinOneClass() {
        when(repo.searchFullText(anyString(), anyInt())).thenReturn(List.of(
                record(6L, "A", AmountClass.OBLIGATED, "10"),
                record(7L, "B", AmountClass.OBLIGATED, "20")));

        SearchResponse res = service.search("bridge", null);

        assertFalse(res.totalShown());
        assertEquals(1, res.amountClassesPresent().size());
        assertTrue(res.whyNoTotal().contains("count money twice"));
    }

    @Test
    @DisplayName("An unpublished figure stays null and never becomes zero")
    void nullAmountStaysNull() {
        when(repo.searchFullText(anyString(), anyInt()))
                .thenReturn(List.of(record(8L, "No figure published", AmountClass.UNKNOWN, null)));

        SearchResponse res = service.search("bridge", null);

        assertEquals(null, res.hits().get(0).amount());
        assertEquals("UNKNOWN", res.hits().get(0).amountClass());
    }

    @Test
    @DisplayName("A one-character query searches nothing")
    void tooShort() {
        SearchResponse res = service.search("a", null);

        assertEquals(0, res.total());
        verify(repo, never()).searchFullText(anyString(), anyInt());
    }

    @Test
    @DisplayName("A record found by two rungs is kept once, under the stronger rung")
    void dedupesKeepingStrongerRung() {
        NormalizedFundingRecord same = record(9L, "Trout River Bridge", AmountClass.OBLIGATED, "5");
        when(repo.searchByIdentifier(anyString(), anyInt())).thenReturn(List.of(same));
        when(repo.searchFullText(anyString(), anyInt())).thenReturn(List.of(same));

        SearchResponse res = service.search("4374372", null);

        assertEquals(1, res.total());
        assertEquals("IDENTIFIER", res.hits().get(0).matchKind());
    }
}
