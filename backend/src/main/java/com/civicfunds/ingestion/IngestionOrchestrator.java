package com.civicfunds.ingestion;

import com.civicfunds.common.text.CanonicalJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Runs one adapter and stores what it returned.
 *
 * <p>Storage is idempotent by construction: the checksum of the canonical
 * payload, together with the source system and the source's record id, forms a
 * unique key. Re-running the same ingestion stores nothing new. Re-running it
 * after the source changed a record stores a second row beside the first,
 * because {@link RawFundingRecord} is append-only - the old version is never
 * lost, and any conclusion drawn from it stays reproducible.
 */
@Service
public class IngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestrator.class);

    private final Map<SourceSystem, FundingSourceAdapter> adapters;
    private final IngestionBatchRepository batchRepository;
    private final RawFundingRecordRepository rawRepository;
    private final ObjectMapper objectMapper;

    public IngestionOrchestrator(List<FundingSourceAdapter> adapterList,
                                 IngestionBatchRepository batchRepository,
                                 RawFundingRecordRepository rawRepository,
                                 ObjectMapper objectMapper) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(FundingSourceAdapter::sourceSystem, Function.identity()));
        this.batchRepository = batchRepository;
        this.rawRepository = rawRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestionBatch run(SourceSystem system, IngestionRequest request) {
        FundingSourceAdapter adapter = adapters.get(system);
        if (adapter == null) {
            throw new IngestionException(system, "No adapter registered for " + system);
        }

        IngestionBatch batch = new IngestionBatch(system.name(), adapter.describeRequest(request));
        batchRepository.save(batch);

        List<RawRecordCandidate> candidates;
        try {
            candidates = adapter.fetch(request);
        } catch (RuntimeException e) {
            batch.setStatus(IngestionBatch.Status.FAILED);
            batch.setErrorMessage(truncate(e.getMessage()));
            batch.setFinishedAt(Instant.now());
            log.error("Ingestion from {} failed", system, e);
            return batchRepository.save(batch);
        }

        batch.setRecordsReceived(candidates.size());
        int stored = 0, duplicate = 0, failed = 0;

        for (RawRecordCandidate candidate : candidates) {
            try {
                String checksum = CanonicalJson.checksum(
                        objectMapper, objectMapper.readTree(candidate.payloadJson()));

                boolean alreadyHave = rawRepository
                        .findBySourceSystemAndSourceRecordIdAndChecksum(
                                system.name(), candidate.sourceRecordId(), checksum)
                        .isPresent();

                if (alreadyHave) {
                    duplicate++;
                    continue;
                }

                rawRepository.save(new RawFundingRecord(
                        batch, system.name(), candidate.sourceRecordId(),
                        candidate.sourceUrl(), candidate.retrievedAt(),
                        candidate.payloadJson(), checksum));
                stored++;
            } catch (Exception e) {
                failed++;
                log.warn("Could not store record {} from {}: {}",
                        candidate.sourceRecordId(), system, e.toString());
            }
        }

        batch.setRecordsStored(stored);
        batch.setRecordsDuplicate(duplicate);
        batch.setRecordsFailed(failed);
        batch.setStatus(failed > 0 ? IngestionBatch.Status.PARTIAL : IngestionBatch.Status.COMPLETED);
        batch.setFinishedAt(Instant.now());

        log.info("Ingested {} from {}: {} received, {} stored, {} duplicate, {} failed",
                batch.getId(), system, candidates.size(), stored, duplicate, failed);
        return batchRepository.save(batch);
    }

    private static String truncate(String message) {
        if (message == null) return "unknown error";
        return message.length() > 1800 ? message.substring(0, 1800) + "…" : message;
    }
}
