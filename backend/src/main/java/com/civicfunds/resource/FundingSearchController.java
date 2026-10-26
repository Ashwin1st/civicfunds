package com.civicfunds.resource;

import com.civicfunds.resource.dto.SearchResponse;
import com.civicfunds.search.FundingSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search — step one of the product flow, and the only entry point a resident
 * needs to know about.
 *
 * <p>Public and unauthenticated, like everything on the read path: these are
 * public government records and there is nothing here to protect.
 */
@RestController
@RequestMapping("/api/funding")
@Validated
@Tag(name = "Search", description = "Find indexed funding records by place, project, agency or government number")
public class FundingSearchController {

    private final FundingSearchService service;

    public FundingSearchController(FundingSearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    @Operation(summary = "Search the indexed records",
            description = "Tries three rungs in order — exact government identifier, "
                    + "weighted full text, then trigram similarity — and tells you which "
                    + "one found each hit. Never returns a combined total.")
    public SearchResponse search(
            @RequestParam("q") @Size(max = 200) String q,
            @RequestParam(value = "limit", required = false)
            @Min(1) @Max(FundingSearchService.MAX_LIMIT) Integer limit) {
        return service.search(q, limit);
    }
}
