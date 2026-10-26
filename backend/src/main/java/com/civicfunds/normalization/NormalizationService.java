package com.civicfunds.normalization;

import com.civicfunds.ingestion.RawFundingRecord;
import com.civicfunds.ingestion.RawFundingRecordRepository;
import com.civicfunds.ingestion.SourceSystem;
import com.civicfunds.ingestion.adapters.usaspending.UsaSpendingNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The second half of the pipeline: turns stored raw payloads into normalized
 * records that the search index can see.
 *
 * <p><b>Why this is a separate pass rather than part of ingestion.</b> Raw
 * storage must not fail because a mapping is wrong. If normalization ran
 * inside the ingestion transaction, one unparseable row would roll back an
 * entire fetch of thousands, and a fix to the mapping would mean re-fetching
 * from the government API instead of re-reading rows we already hold. Keeping
 * them apart means the expensive, rate-limited, externally-dependent step
 * happens once, and the cheap, frequently-corrected step can be re-run as
 * often as the mapping changes.
 *
 * <p>Consequence worth stating plainly: after an ingestion, nothing is
 * searchable until this runs. That is by design, not an oversight, but it does
 * mean a demo needs both steps.
 */
@Service
public class NormalizationService {

    private static final Logger log = LoggerFactory.getLogger(NormalizationService.class);

    /** How many raw rows to map per transaction. Keeps one bad batch small. */
    private static final int BATCH = 500;

    private final RawFundingRecordRepository rawRepository;
    private final NormalizedFundingRecordRepository normalizedRepository;
    private final UsaSpendingNormalizer usaSpendingNormalizer;

    public NormalizationService(RawFundingRecordRepository rawRepository,
                                NormalizedFundingRecordRepository normalizedRepository,
                                UsaSpendingNormalizer usaSpendingNormalizer) {
        this.rawRepository = rawRepository;
        this.normalizedRepository = normalizedRepository;
        this.usaSpendingNormalizer = usaSpendingNormalizer;
    }

    /** What one normalization pass did. Reported to the operator, not inferred. */
    public record Result(String sourceSystem,
                         long pendingBefore,
                         int written,
                         int failed,
                         long pendingAfter,
                         List<String> errors) {
    }

    /**
     * Normalizes every raw record of one source that does not yet have a
     * normalized counterpart. Safe to run repeatedly: a second run with
     * nothing pending writes nothing.
     */
    @Transactional
    public Result normalizePending(SourceSystem system, int maxRecords) {
        if (system != SourceSystem.USASPENDING) {
            throw new IllegalArgumentException(
                    "No normalizer is registered for " + system + ". CivicFunds currently "
                    + "normalizes USAspending only; the other sources are not ingested yet.");
        }

        long pendingBefore = rawRepository.countUnnormalized(system.name());
        int limit = maxRecords > 0 ? Math.min(maxRecords, (int) Math.min(pendingBefore, Integer.MAX_VALUE))
                                   : (int) Math.min(pendingBefore, Integer.MAX_VALUE);

        int written = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        // A row that fails to map stays unnormalized, so the pending query would
        // hand it back on every pass. Remembering what has been attempted is what
        // stops this loop spinning on the same broken row forever.
        Set<Long> attempted = new HashSet<>();

        while (written + failed < limit) {
            // Ask for a batch plus room for everything already attempted and
            // skipped, so a growing tail of failures cannot starve the window.
            int window = Math.min(BATCH + attempted.size(), Integer.MAX_VALUE - 1);
            List<RawFundingRecord> page =
                    rawRepository.findUnnormalized(system.name(), PageRequest.of(0, window));

            List<RawFundingRecord> batch = page.stream()
                    .filter(r -> !attempted.contains(r.getId()))
                    .limit(Math.min(BATCH, limit - written - failed))
                    .toList();

            if (batch.isEmpty()) {
                break;   // nothing left that has not already been tried
            }

            for (RawFundingRecord raw : batch) {
                attempted.add(raw.getId());
                try {
                    NormalizedFundingRecord normalized = usaSpendingNormalizer.normalize(raw);
                    normalizedRepository.save(normalized);
                    written++;
                } catch (Exception e) {
                    failed++;
                    // A row that will not map is a mapping bug to fix, not a reason
                    // to abandon the other 19,999. Record it and carry on.
                    String message = "raw id " + raw.getId() + ": " + e.getClass().getSimpleName()
                                     + " " + e.getMessage();
                    if (errors.size() < 20) {
                        errors.add(message);
                    }
                    log.warn("Could not normalize {}", message);
                }
            }
        }

        long pendingAfter = rawRepository.countUnnormalized(system.name());
        log.info("Normalized {}: {} written, {} failed, {} still pending",
                system, written, failed, pendingAfter);

        return new Result(system.name(), pendingBefore, written, failed, pendingAfter, errors);
    }
}
