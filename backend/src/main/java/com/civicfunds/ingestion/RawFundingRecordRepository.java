package com.civicfunds.ingestion;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RawFundingRecordRepository extends JpaRepository<RawFundingRecord, Long> {

    /** Idempotency check: have we already stored this exact payload? */
    Optional<RawFundingRecord> findBySourceSystemAndSourceRecordIdAndChecksum(
            String sourceSystem, String sourceRecordId, String checksum);

    List<RawFundingRecord> findBySourceSystemAndSourceRecordId(
            String sourceSystem, String sourceRecordId);

    long countBySourceSystem(String sourceSystem);

    @Query("select distinct r.sourceSystem from RawFundingRecord r")
    List<String> findDistinctSourceSystems();

    /**
     * Raw records from one source that have not yet been normalized.
     *
     * <p>This is the join that makes normalization restartable. Raw storage is
     * append-only and normalization is a separate pass, so the two can drift:
     * an ingestion that succeeds and a normalization that crashes halfway
     * leaves rows here with no counterpart. Asking the database which rows
     * lack a counterpart — rather than tracking a cursor — means the pass can
     * be re-run any number of times and always does exactly the work left.
     */
    @Query("""
           select r from RawFundingRecord r
           where r.sourceSystem = :system
             and not exists (
                 select 1 from NormalizedFundingRecord n where n.rawRecord = r)
           order by r.id
           """)
    List<RawFundingRecord> findUnnormalized(@Param("system") String system, Pageable page);

    @Query("""
           select count(r) from RawFundingRecord r
           where r.sourceSystem = :system
             and not exists (
                 select 1 from NormalizedFundingRecord n where n.rawRecord = r)
           """)
    long countUnnormalized(@Param("system") String system);
}
