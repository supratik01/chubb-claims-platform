package com.chubb.claims.support;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.domain.model.Policy;
import com.chubb.claims.domain.model.Role;
import com.chubb.claims.domain.repository.PolicyRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.port.PasswordHasher;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Base class for tests that run the whole application against real PostgreSQL and Kafka containers.
 *
 * <p>Every request made through {@link #call} is checked against the OpenAPI contract, so a controller that
 * drifts from {@code claims-api.yaml} fails these tests. Each test creates its own users and policies, so
 * tests do not depend on each other or on seed data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "integration-test-password";

    /**
     * ClaimDetail is composed with allOf. The validator checks each part of an allOf on its own and would
     * report the other part's fields as unknown, so that one check is switched off, as its documentation advises.
     */
    private static final OpenApiInteractionValidator CONTRACT =
            OpenApiInteractionValidator.createForInlineApiSpecification(readContract())
                    .withLevelResolver(LevelResolver.create()
                            .withLevel("validation.schema.additionalProperties", ValidationReport.Level.IGNORE)
                            .build())
                    .build();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    private UserRepository users;
    @Autowired
    private PolicyRepository policies;
    @Autowired
    private PasswordHasher passwordHasher;

    /** A user created for one test, already signed in. */
    public record TestUser(UUID id, String username, String displayName, String token) {
    }

    protected TestUser signedIn(Role role) throws Exception {
        String username = role.name().toLowerCase() + "-" + UUID.randomUUID();
        AppUser user = users.save(new AppUser(UUID.randomUUID(), username, passwordHasher.hash(PASSWORD),
                "Test " + role.name().toLowerCase().replace('_', ' ') + " " + username.substring(username.length() - 4),
                role));
        JsonNode session = body(call(null, post("/api/v1/auth/login"),
                Map.of("username", username, "password", PASSWORD)));
        return new TestUser(user.getId(), username, user.getDisplayName(), session.get("accessToken").asString());
    }

    protected UUID policyFor(TestUser holder, String countryCode, String currency, String sumInsured) {
        AppUser user = users.findById(holder.id()).orElseThrow();
        Policy policy = policies.save(new Policy(UUID.randomUUID(), "POL-" + UUID.randomUUID().toString().substring(0, 8),
                user, LineOfBusiness.MOTOR, countryCode, currency, new BigDecimal(sumInsured),
                LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)));
        return policy.getId();
    }

    protected Map<String, Object> claimRequest(UUID policyId, String amount) {
        return Map.of(
                "policyId", policyId,
                "incidentDate", LocalDate.now().minusDays(2).toString(),
                "incidentLocation", "Test Street",
                "description", "Rear-ended at a traffic light on the way to work",
                "claimedAmount", new BigDecimal(amount));
    }

    /** Submits a claim as the claimant and returns its id. */
    protected String submitClaim(TestUser claimant, UUID policyId, String amount) throws Exception {
        return body(call(claimant, post("/api/v1/claims"), claimRequest(policyId, amount))).get("id").asString();
    }

    protected ResultActions getAs(TestUser user, String url, Object... uriVariables) throws Exception {
        return call(user, get(url, uriVariables), null);
    }

    protected ResultActions postAs(TestUser user, String url, Object requestBody, Object... uriVariables)
            throws Exception {
        return call(user, post(url, uriVariables), requestBody);
    }

    /** Performs the request and asserts that the request and response conform to the OpenAPI contract. */
    protected ResultActions call(TestUser user, MockHttpServletRequestBuilder request, Object requestBody)
            throws Exception {
        if (user != null) {
            request.header("Authorization", "Bearer " + user.token());
        }
        if (requestBody != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(requestBody));
        }
        return mvc.perform(request).andExpect(openApi().isValid(CONTRACT));
    }

    protected JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static String readContract() {
        try {
            return new ClassPathResource("openapi/claims-api.yaml").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
