package com.chubb.claims.api;

import com.chubb.claims.api.dto.ClaimDetailResponse;
import com.chubb.claims.api.dto.ClaimPageResponse;
import com.chubb.claims.api.dto.SubmitClaimRequest;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.service.Actor;
import com.chubb.claims.service.ClaimQueryService;
import com.chubb.claims.service.ClaimSubmissionService;
import com.chubb.claims.service.command.AssignmentFilter;
import com.chubb.claims.service.view.ClaimDetailView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reporting an incident and reading claims. */
@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimSubmissionService submission;
    private final ClaimQueryService queries;

    public ClaimController(ClaimSubmissionService submission, ClaimQueryService queries) {
        this.submission = submission;
        this.queries = queries;
    }

    @PostMapping
    public ResponseEntity<ClaimDetailResponse> submitClaim(Actor actor, @Valid @RequestBody SubmitClaimRequest request) {
        ClaimDetailView claim = submission.submit(actor, request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/v1/claims/" + claim.summary().id()))
                .body(ClaimDetailResponse.from(claim));
    }

    @GetMapping
    public ClaimPageResponse listClaims(
            Actor actor,
            @RequestParam(defaultValue = "ALL") AssignmentFilter assignment,
            @RequestParam(required = false) Set<ClaimStatus> status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ClaimPageResponse.from(queries.search(actor, assignment, status, page, size));
    }

    @GetMapping("/{claimId}")
    public ClaimDetailResponse getClaim(Actor actor, @PathVariable UUID claimId) {
        return ClaimDetailResponse.from(queries.get(actor, claimId));
    }
}
