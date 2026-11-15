package com.chubb.claims.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chubb.claims.domain.model.Role;
import com.chubb.claims.support.IntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClaimLifecycleIntegrationTest extends IntegrationTest {

    @Test
    void aClaimTravelsFromReportToSettlementThroughTheApi() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        TestUser officer = signedIn(Role.CLAIMS_OFFICER);
        UUID policyId = policyFor(claimant, "SG", "SGD", "50000.00");

        // The claimant reports an incident.
        var created = postAs(claimant, "/api/v1/claims", claimRequest(policyId, "4200.00"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.currency").value("SGD"))
                .andExpect(jsonPath("$.reserveAmount").value(nullValue()));
        String claimId = body(created).get("id").asString();

        // It appears in the queue of unassigned claims, and the officer picks it up.
        getAs(officer, "/api/v1/claims?assignment=UNASSIGNED&status=SUBMITTED&size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(hasItem(claimId)));
        postAs(officer, "/api/v1/claims/{id}/assignment", null, claimId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.assignedOfficerId").value(officer.id().toString()))
                .andExpect(jsonPath("$.reserveAmount").value(4200.00));
        getAs(officer, "/api/v1/claims?assignment=MINE")
                .andExpect(jsonPath("$.items[*].id").value(contains(claimId)));

        // The officer asks a question; the claimant sees it and answers.
        postAs(officer, "/api/v1/claims/{id}/information-requests",
                Map.of("question", "Please send the repair quote"), claimId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INFO_REQUESTED"));
        var asSeenByClaimant = getAs(claimant, "/api/v1/claims/{id}", claimId)
                .andExpect(jsonPath("$.informationRequests[0].question").value("Please send the repair quote"));
        String requestId = body(asSeenByClaimant).get("informationRequests").get(0).get("id").asString();
        postAs(claimant, "/api/v1/claims/{id}/information-requests/{requestId}/response",
                Map.of("response", "The workshop quoted SGD 4,200"), claimId, requestId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        // The officer approves a lower amount; the claimant sees the decision; the officer records payment.
        postAs(officer, "/api/v1/claims/{id}/decision",
                Map.of("outcome", "APPROVED", "approvedAmount", 3800.00, "reason", "Excess deducted"), claimId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        getAs(claimant, "/api/v1/claims/{id}", claimId)
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAmount").value(3800.00))
                .andExpect(jsonPath("$.decisionReason").value("Excess deducted"));
        postAs(officer, "/api/v1/claims/{id}/settlement", null, claimId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.statusHistory[*].toStatus").value(contains(
                        "SUBMITTED", "UNDER_REVIEW", "INFO_REQUESTED", "UNDER_REVIEW", "APPROVED", "SETTLED")));
    }

    @Test
    void aClaimantSeesOnlyTheirOwnClaims() throws Exception {
        TestUser owner = signedIn(Role.CLAIMANT);
        TestUser stranger = signedIn(Role.CLAIMANT);
        String claimId = submitClaim(owner, policyFor(owner, "SG", "SGD", "50000.00"), "900.00");

        getAs(stranger, "/api/v1/claims/{id}", claimId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        getAs(stranger, "/api/v1/claims")
                .andExpect(jsonPath("$.totalItems").value(0));
        getAs(owner, "/api/v1/claims")
                .andExpect(jsonPath("$.items[*].id").value(contains(claimId)));
    }

    @Test
    void aClaimantCanOnlyClaimAgainstTheirOwnPolicy() throws Exception {
        TestUser owner = signedIn(Role.CLAIMANT);
        TestUser stranger = signedIn(Role.CLAIMANT);
        UUID ownersPolicy = policyFor(owner, "SG", "SGD", "50000.00");

        postAs(stranger, "/api/v1/claims", claimRequest(ownersPolicy, "900.00"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
        getAs(owner, "/api/v1/policies")
                .andExpect(jsonPath("$[*].id").value(contains(ownersPolicy.toString())));
        getAs(stranger, "/api/v1/policies")
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aClaimThatIsAlreadyPickedUpCannotBePickedUpAgain() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        String claimId = submitClaim(claimant, policyFor(claimant, "SG", "SGD", "50000.00"), "900.00");
        postAs(signedIn(Role.CLAIMS_OFFICER), "/api/v1/claims/{id}/assignment", null, claimId)
                .andExpect(status().isOk());

        postAs(signedIn(Role.CLAIMS_OFFICER), "/api/v1/claims/{id}/assignment", null, claimId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void onlyTheAssignedOfficerOrAManagerCanDecideAClaim() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        String claimId = submitClaim(claimant, policyFor(claimant, "SG", "SGD", "50000.00"), "900.00");
        postAs(signedIn(Role.CLAIMS_OFFICER), "/api/v1/claims/{id}/assignment", null, claimId);
        Map<String, Object> rejection = Map.of("outcome", "REJECTED", "reason", "Not covered by the policy");

        postAs(signedIn(Role.CLAIMS_OFFICER), "/api/v1/claims/{id}/decision", rejection, claimId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        postAs(signedIn(Role.CLAIMS_MANAGER), "/api/v1/claims/{id}/decision", rejection, claimId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionReason").value("Not covered by the policy"));
    }

    @Test
    void approvingMoreThanTheSumInsuredIsRefusedAndTheClaimIsUnchanged() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        TestUser officer = signedIn(Role.CLAIMS_OFFICER);
        String claimId = submitClaim(claimant, policyFor(claimant, "SG", "SGD", "1000.00"), "900.00");
        postAs(officer, "/api/v1/claims/{id}/assignment", null, claimId);

        postAs(officer, "/api/v1/claims/{id}/decision",
                Map.of("outcome", "APPROVED", "approvedAmount", 1000.01), claimId)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
        getAs(officer, "/api/v1/claims/{id}", claimId)
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    void theWorkloadViewShowsWhatEachOfficerIsHolding() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        TestUser officer = signedIn(Role.CLAIMS_OFFICER);
        UUID policyId = policyFor(claimant, "SG", "SGD", "50000.00");
        String underReview = submitClaim(claimant, policyId, "100.00");
        String awaitingInfo = submitClaim(claimant, policyId, "200.00");
        postAs(officer, "/api/v1/claims/{id}/assignment", null, underReview);
        postAs(officer, "/api/v1/claims/{id}/assignment", null, awaitingInfo);
        postAs(officer, "/api/v1/claims/{id}/information-requests",
                Map.of("question", "Where did it happen?"), awaitingInfo);

        String mine = "$.officers[?(@.officerId == '" + officer.id() + "')]";
        getAs(officer, "/api/v1/dashboard/workload")
                .andExpect(status().isOk())
                .andExpect(jsonPath(mine + ".underReview").value(contains(1)))
                .andExpect(jsonPath(mine + ".awaitingInformation").value(contains(1)))
                .andExpect(jsonPath(mine + ".closed").value(contains(0)))
                .andExpect(jsonPath("$.unassignedCount").value(not(nullValue())));
    }
}
