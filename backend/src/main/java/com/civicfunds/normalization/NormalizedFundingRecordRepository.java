package com.civicfunds.normalization;

import com.civicfunds.common.GovernmentLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NormalizedFundingRecordRepository
        extends JpaRepository<NormalizedFundingRecord, Long> {

    List<NormalizedFundingRecord> findByExternalKey(String externalKey);

    List<NormalizedFundingRecord> findByGovernmentLevel(GovernmentLevel level);

    List<NormalizedFundingRecord> findByCouncilDistrict(String councilDistrict);

    long countByGovernmentLevel(GovernmentLevel level);

    @Query("select count(distinct n.sourceSystem) from NormalizedFundingRecord n")
    long countDistinctSourceSystems();

    @Query("select count(n) from NormalizedFundingRecord n where n.externalKey is not null")
    long countWithDeterministicKey();

    // ------------------------------------------------------------------
    // Overview counts. Every one of these counts RECORDS, never dollars.
    // Summing amounts across any of these groupings would be the exact
    // mistake this application exists to refuse, so the queries simply
    // cannot do it: there is no SUM in this file.
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT government_level, count(*) FROM normalized_funding_record
            GROUP BY 1 ORDER BY 2 DESC
            """, nativeQuery = true)
    List<Object[]> tallyByGovernmentLevel();

    @Query(value = """
            SELECT amount_class, count(*) FROM normalized_funding_record
            GROUP BY 1 ORDER BY 2 DESC
            """, nativeQuery = true)
    List<Object[]> tallyByMeasurementClass();

    @Query(value = """
            SELECT source_system, count(*) FROM normalized_funding_record
            GROUP BY 1 ORDER BY 2 DESC
            """, nativeQuery = true)
    List<Object[]> tallyBySourceSystem();

    @Query(value = """
            SELECT coalesce(category, 'Not categorised'), count(*)
            FROM normalized_funding_record
            GROUP BY 1 ORDER BY 2 DESC LIMIT 12
            """, nativeQuery = true)
    List<Object[]> tallyByCategory();

    /**
     * Records with no published city are counted and labelled, not dropped.
     * A gap in the source is a fact about the source.
     */
    @Query(value = """
            SELECT coalesce(nullif(trim(city), ''), 'No city published'), count(*)
            FROM normalized_funding_record
            GROUP BY 1 ORDER BY 2 DESC LIMIT 10
            """, nativeQuery = true)
    List<Object[]> tallyByCity();

    // ------------------------------------------------------------------
    // Search. Three rungs, deliberately three separate queries rather than
    // one clever UNION: the service needs to know WHICH rung produced each
    // hit so the interface can say so, and a combined query loses that.
    // Native SQL because tsvector, ts_rank and pg_trgm have no JPQL.
    // ------------------------------------------------------------------

    /**
     * Rung 1 - exact identifier. Case-insensitive, matched against both the
     * source's own project key and the award id. Nothing is scored here.
     */
    @Query(value = """
            SELECT * FROM normalized_funding_record
            WHERE upper(external_key) = :key OR upper(award_id) = :key
            ORDER BY id
            LIMIT :max
            """, nativeQuery = true)
    List<NormalizedFundingRecord> searchByIdentifier(@Param("key") String key,
                                                     @Param("max") int max);

    /**
     * Rung 2 - weighted full text. A hit in the project title outranks a hit
     * in a party name, which outranks a city name (see V6 for the weights).
     */
    @Query(value = """
            SELECT * FROM normalized_funding_record
            WHERE search_vector @@ plainto_tsquery('english', :q)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :q)) DESC, id
            LIMIT :max
            """, nativeQuery = true)
    List<NormalizedFundingRecord> searchFullText(@Param("q") String q,
                                                 @Param("max") int max);

    /**
     * Rung 3 - trigram similarity, for misspellings and abbreviations. The
     * {@code %} operator uses pg_trgm's similarity threshold (0.3 by default).
     */
    @Query(value = """
            SELECT * FROM normalized_funding_record
            WHERE project_title % :q OR recipient_raw % :q OR funding_agency_raw % :q
            ORDER BY GREATEST(
                         similarity(coalesce(project_title,      ''), :q),
                         similarity(coalesce(recipient_raw,      ''), :q),
                         similarity(coalesce(funding_agency_raw, ''), :q)) DESC, id
            LIMIT :max
            """, nativeQuery = true)
    List<NormalizedFundingRecord> searchFuzzy(@Param("q") String q,
                                              @Param("max") int max);
}
