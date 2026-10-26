package com.civicfunds.resource;

import com.civicfunds.common.GovernmentLevel;
import com.civicfunds.common.money.AmountClass;
import com.civicfunds.ingestion.RawFundingRecordRepository;
import com.civicfunds.normalization.NormalizedFundingRecordRepository;
import com.civicfunds.resource.dto.CoverageResponse;
import com.civicfunds.resource.dto.GlossaryTerm;
import com.civicfunds.resource.dto.OverviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Meta", description = "What is indexed, and what the words mean")
public class MetaController {

    private final RawFundingRecordRepository rawRepo;
    private final NormalizedFundingRecordRepository normRepo;

    public MetaController(RawFundingRecordRepository rawRepo,
                          NormalizedFundingRecordRepository normRepo) {
        this.rawRepo = rawRepo;
        this.normRepo = normRepo;
    }

    @GetMapping("/meta/coverage")
    @Operation(summary = "What data is currently indexed, from where, and what is known to be missing")
    public CoverageResponse coverage() {
        Map<GovernmentLevel, Long> byLevel = new EnumMap<>(GovernmentLevel.class);
        for (GovernmentLevel level : GovernmentLevel.values()) {
            byLevel.put(level, normRepo.countByGovernmentLevel(level));
        }
        return new CoverageResponse(
                rawRepo.count(),
                normRepo.count(),
                normRepo.countWithDeterministicKey(),
                rawRepo.findDistinctSourceSystems(),
                byLevel);
    }

    @GetMapping("/meta/overview")
    @Operation(summary = "Record counts by level, measurement class, source, category and place",
               description = "Counts of records only. No dollar total is returned, because the "
                           + "records carry several measurement classes and a sum across them "
                           + "would describe nothing that exists.")
    public OverviewResponse overview() {
        List<OverviewResponse.Bucket> classes = buckets(normRepo.tallyByMeasurementClass());
        return new OverviewResponse(
                normRepo.count(),
                rawRepo.findDistinctSourceSystems().size(),
                normRepo.countWithDeterministicKey(),
                classes.size(),
                buckets(normRepo.tallyByGovernmentLevel()),
                classes,
                buckets(normRepo.tallyBySourceSystem()),
                buckets(normRepo.tallyByCategory()),
                buckets(normRepo.tallyByCity()));
    }

    /** Native count queries return Object[]; the driver's integer type varies by version. */
    private static List<OverviewResponse.Bucket> buckets(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new OverviewResponse.Bucket(
                        r[0] == null ? "Not recorded" : r[0].toString(),
                        ((Number) r[1]).longValue()))
                .toList();
    }

    @GetMapping("/glossary/terms")
    @Operation(summary = "Definitions for every amount class, served from one place so tooltips cannot drift")
    public List<GlossaryTerm> glossary() {
        return Arrays.stream(AmountClass.values())
                .map(c -> new GlossaryTerm(c.name(), c.label(), c.definition(), c.isSummable()))
                .toList();
    }
}
