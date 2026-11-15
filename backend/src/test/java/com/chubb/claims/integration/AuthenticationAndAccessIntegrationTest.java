package com.chubb.claims.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chubb.claims.domain.model.Role;
import com.chubb.claims.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthenticationAndAccessIntegrationTest extends IntegrationTest {

    @Test
    void signingInReturnsATokenAndTheUsersRole() throws Exception {
        TestUser officer = signedIn(Role.CLAIMS_OFFICER);

        postAs(null, "/api/v1/auth/login", Map.of("username", officer.username(), "password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("CLAIMS_OFFICER"))
                .andExpect(jsonPath("$.user.id").value(officer.id().toString()));
    }

    @Test
    void aWrongPasswordIsRefusedWithAProblemResponse() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);

        postAs(null, "/api/v1/auth/login", Map.of("username", claimant.username(), "password", "not-the-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void aRequestWithoutATokenIsRefusedWithAProblemResponse() throws Exception {
        mvc.perform(get("/api/v1/claims"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void aTokenThatWasTamperedWithIsRefused() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);

        mvc.perform(get("/api/v1/claims").header("Authorization", "Bearer " + claimant.token() + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void aClaimantCannotUseStaffOperations() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);

        postAs(claimant, "/api/v1/claims/{id}/assignment", null, UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        getAs(claimant, "/api/v1/dashboard/workload")
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotReportAnIncident() throws Exception {
        TestUser officer = signedIn(Role.CLAIMS_OFFICER);

        postAs(officer, "/api/v1/claims", claimRequest(UUID.randomUUID(), "100.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exposureIsVisibleToManagersButNotToOfficers() throws Exception {
        getAs(signedIn(Role.CLAIMS_OFFICER), "/api/v1/dashboard/exposure").andExpect(status().isForbidden());
        getAs(signedIn(Role.CLAIMS_MANAGER), "/api/v1/dashboard/exposure").andExpect(status().isOk());
    }

    @Test
    void invalidInputIsReportedFieldByFieldInTheProblemResponse() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        Map<String, Object> invalid = new HashMap<>(claimRequest(UUID.randomUUID(), "-5.00"));
        invalid.put("description", "short");

        // Sent with plain MockMvc: the request is deliberately outside the contract.
        mvc.perform(post("/api/v1/claims")
                        .header("Authorization", "Bearer " + claimant.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("description", "claimedAmount")));
    }

    @Test
    void anUnreadableRequestBodyUsesTheSameProblemFormat() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);

        mvc.perform(post("/api/v1/claims")
                        .header("Authorization", "Bearer " + claimant.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
