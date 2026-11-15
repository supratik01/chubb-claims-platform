package com.chubb.claims.api;

import com.chubb.claims.api.dto.ClaimDetailResponse;
import com.chubb.claims.api.dto.CreateInformationRequest;
import com.chubb.claims.api.dto.DecisionRequest;
import com.chubb.claims.api.dto.InformationResponse;
import com.chubb.claims.service.Actor;
import com.chubb.claims.service.ClaimWorkflowService;
import com.chubb.claims.service.view.ClaimDetailView;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The actions that move a claim through its workflow. Each returns the claim as it is after the change. */
@RestController
@RequestMapping("/api/v1/claims/{claimId}")
public class ClaimWorkflowController {

    private final ClaimWorkflowService workflow;

    public ClaimWorkflowController(ClaimWorkflowService workflow) {
        this.workflow = workflow;
    }

    @PostMapping("/assignment")
    public ClaimDetailResponse pickUpClaim(Actor actor, @PathVariable UUID claimId) {
        return ClaimDetailResponse.from(workflow.pickUp(actor, claimId));
    }

    @PostMapping("/information-requests")
    public ClaimDetailResponse requestInformation(Actor actor, @PathVariable UUID claimId,
                                                  @Valid @RequestBody CreateInformationRequest request) {
        return ClaimDetailResponse.from(workflow.requestInformation(actor, claimId, request.question()));
    }

    @PostMapping("/information-requests/{requestId}/response")
    public ClaimDetailResponse respondToInformationRequest(Actor actor, @PathVariable UUID claimId,
                                                           @PathVariable UUID requestId,
                                                           @Valid @RequestBody InformationResponse request) {
        return ClaimDetailResponse.from(
                workflow.answerInformationRequest(actor, claimId, requestId, request.response()));
    }

    @PostMapping("/decision")
    public ClaimDetailResponse decideClaim(Actor actor, @PathVariable UUID claimId,
                                           @Valid @RequestBody DecisionRequest request) {
        ClaimDetailView claim = switch (request.outcome()) {
            case APPROVED -> workflow.approve(actor, claimId, request.approvedAmount(), request.reason());
            case REJECTED -> workflow.reject(actor, claimId, request.reason());
        };
        return ClaimDetailResponse.from(claim);
    }

    @PostMapping("/settlement")
    public ClaimDetailResponse settleClaim(Actor actor, @PathVariable UUID claimId) {
        return ClaimDetailResponse.from(workflow.settle(actor, claimId));
    }
}
