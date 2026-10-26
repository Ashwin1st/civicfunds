package com.civicfunds.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionBatchRepository extends JpaRepository<IngestionBatch, Long> {

    List<IngestionBatch> findTop20ByOrderByStartedAtDesc();

    List<IngestionBatch> findBySourceSystemOrderByStartedAtDesc(String sourceSystem);
}
