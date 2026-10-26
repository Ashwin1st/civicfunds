package com.civicfunds.search;

import com.civicfunds.normalization.NormalizedFundingRecord;
import com.civicfunds.normalization.NormalizedFundingRecordRepository;
import com.civicfunds.resource.dto.SearchHit;
import com.civicfunds.resource.dto.SearchResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Search over the indexed records.
 *
 * <p>Three rungs, tried in order, each one weaker than the last:
 *
 * <ol>
 *   <li><b>Identifier</b> — the query is a government key, so the answer is a
 *       lookup. Exact, or nothing.</li>
 *   <li><b>Full text</b> — the words are actually in the record.</li>
 *   <li><b>Trigram</b> — the words are not in the record but something close
 *       is. Only tried if the first two left room.</li>
 * </ol>
 *
 * <p>Every hit remembers which rung found it, and that travels all the way to
 * the screen. The ordering is not an optimisation; it is a claim about
 * confidence, and the interface repeats it.
 *
 * <p>No rung sums anything. {@link SearchResponse#totalShown()} is always
 * false, and when more than one measurement class is present among the hits
 * the response says so explicitly.
 */
@Service
public class FundingSearchService {

    /** Defensive ceiling so a one-letter query cannot drag the whole table back. */
    public static final int MAX_LIMIT = 100;
    public static final int DEFAULT_LIMIT = 25;

    private final NormalizedFundingRecordRepository repo;

    public FundingSearchService(NormalizedFundingRecordRepository repo) {
        this.repo = repo;
    }

    public SearchResponse search(String rawQuery, Integer requestedLimit) {
        String q = rawQuery == null ? "" : rawQuery.trim();
        int limit = clamp(requestedLimit);

        if (q.length() < 2) {
            return empty(q, "Type at least two characters.");
        }

        // Keyed by record id, so a record found twice keeps the strongest rung
        // that found it. LinkedHashMap preserves rung order in the output.
        Map<Long, Hit> found = new LinkedHashMap<>();

        Optional<String> key = IdentifierQuery.normalize(q);
        if (key.isPresent()) {
            collect(found, repo.searchByIdentifier(key.get(), limit),
                    MatchKind.IDENTIFIER, limit);
        }

        if (found.size() < limit) {
            collect(found, repo.searchFullText(q, limit), MatchKind.KEYWORD, limit);
        }

        // Rung 3 is a last resort. Running it when the first two already
        // answered would bury good hits under resemblances.
        if (found.isEmpty()) {
            collect(found, repo.searchFuzzy(q, limit), MatchKind.FUZZY, limit);
        }

        List<SearchHit> hits = found.values().stream().map(FundingSearchService::toDto).toList();

        Map<String, Integer> byKind = new LinkedHashMap<>();
        for (Hit h : found.values()) {
            byKind.merge(h.kind().name(), 1, Integer::sum);
        }

        Set<String> classes = new LinkedHashSet<>();
        for (Hit h : found.values()) {
            classes.add(h.record().getAmountClass().name());
        }

        return new SearchResponse(q, hits.size(), byKind, List.copyOf(classes),
                false, whyNoTotal(classes), hits);
    }

    /**
     * The reason is specific to what was actually found, because "we never
     * total" is less useful than "these four figures measure four things".
     */
    private static String whyNoTotal(Set<String> classes) {
        if (classes.isEmpty()) {
            return "Nothing to total.";
        }
        if (classes.size() == 1) {
            return "These records share one measurement class (" + classes.iterator().next()
                    + "), but CivicFunds still shows no total: matching classes is only half "
                    + "the test. Two records can describe the same dollars, and until the "
                    + "reconciler has cleared the relationship between them, adding them would "
                    + "count money twice.";
        }
        return "These records carry " + classes.size() + " different measurement classes ("
                + String.join(", ", classes) + "). They are answers to different questions, "
                + "so adding them would produce a figure that is not true in any sense. "
                + "CivicFunds shows the ledger instead.";
    }

    private void collect(Map<Long, Hit> into, List<NormalizedFundingRecord> rows,
                         MatchKind kind, int limit) {
        for (NormalizedFundingRecord r : rows) {
            if (into.size() >= limit) {
                return;
            }
            into.putIfAbsent(r.getId(), new Hit(r, kind));
        }
    }

    private static int clamp(Integer requested) {
        if (requested == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, requested));
    }

    private SearchResponse empty(String q, String why) {
        return new SearchResponse(q, 0, Map.of(), List.of(), false, why, List.of());
    }

    private static SearchHit toDto(Hit h) {
        NormalizedFundingRecord r = h.record();
        return new SearchHit(
                r.getId(),
                h.kind().name(),
                h.kind().explanation(),
                r.getProjectTitle(),
                r.getSourceSystem(),
                r.getGovernmentLevel() == null ? null : r.getGovernmentLevel().name(),
                // Null stays null. An unpublished figure is not zero.
                r.getAmount() == null ? null : r.getAmount().toPlainString(),
                r.getAmountClass().name(),
                r.getRecipientRaw(),
                r.getFundingAgencyRaw(),
                r.getExternalKey(),
                place(r),
                r.getCouncilDistrict(),
                r.getSourceUrl(),
                r.getRetrievedAt() == null ? null : r.getRetrievedAt().toString());
    }

    /** Whatever place the source actually gave, nothing composed on top of it. */
    private static String place(NormalizedFundingRecord r) {
        List<String> parts = new ArrayList<>();
        if (notBlank(r.getCity())) {
            parts.add(r.getCity());
        }
        if (notBlank(r.getCounty())) {
            parts.add(r.getCounty() + " County");
        }
        if (notBlank(r.getZip5())) {
            parts.add(r.getZip5());
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** A record plus the rung that found it. */
    private record Hit(NormalizedFundingRecord record, MatchKind kind) {
    }
}
