package com.civicfunds.resource;

import com.civicfunds.ingestion.IngestionBatch;
import com.civicfunds.ingestion.IngestionBatchRepository;
import com.civicfunds.ingestion.IngestionOrchestrator;
import com.civicfunds.ingestion.IngestionRequest;
import com.civicfunds.ingestion.SourceSystem;
import com.civicfunds.normalization.NormalizationService;
import com.civicfunds.resource.dto.IngestionBatchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Ingestion is triggered manually. It is a batch job that can run for minutes
 * and must never happen inside a page request, so nothing on the public read
 * path can start one.
 */
@RestController
@RequestMapping("/api/admin/ingestion")
@Tag(name = "Admin — ingestion", description = "Pull records from government sources")
public class AdminIngestionController {

    private final IngestionOrchestrator orchestrator;
    private final IngestionBatchRepository batchRepository;
    private final NormalizationService normalizationService;

    public AdminIngestionController(IngestionOrchestrator orchestrator,
                                    IngestionBatchRepository batchRepository,
                                    NormalizationService normalizationService) {
        this.orchestrator = orchestrator;
        this.batchRepository = batchRepository;
        this.normalizationService = normalizationService;
    }

    @PostMapping("/usaspending")
    @Operation(summary = "Fetch federal awards with a Duval County place of performance")
    public IngestionBatchResponse ingestUsaSpending(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int maxRecords) {

        IngestionBatch batch = orchestrator.run(
                SourceSystem.USASPENDING, new IngestionRequest(from, to, maxRecords));
        return IngestionBatchResponse.from(batch);
    }

    @PostMapping("/normalize")
    @Operation(summary = "Map stored raw records into searchable normalized records",
               description = "Ingestion only stores raw payloads. Nothing is searchable "
                           + "until this runs. Safe to re-run; it only processes rows that "
                           + "do not already have a normalized counterpart.")
    public NormalizationService.Result normalize(
            @RequestParam(defaultValue = "USASPENDING") SourceSystem source,
            @RequestParam(defaultValue = "0") @Min(0) int maxRecords) {

        return normalizationService.normalizePending(source, maxRecords);
    }

    @GetMapping("/batches")
    @Operation(summary = "The 20 most recent ingestion batches")
    public List<IngestionBatchResponse> recentBatches() {
        return batchRepository.findTop20ByOrderByStartedAtDesc()
                .stream().map(IngestionBatchResponse::from).toList();
    }
}
