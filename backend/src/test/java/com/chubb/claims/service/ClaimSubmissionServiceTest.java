package com.chubb.claims.service;

import static com.chubb.claims.support.Fixtures.claimant;
import static com.chubb.claims.support.Fixtures.motorPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chubb.claims.domain.event.ClaimEventPublisher;
import com.chubb.claims.domain.exception.BusinessRuleViolationException;
import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.Policy;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.domain.repository.PolicyRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.command.SubmitClaimCommand;
import com.chubb.claims.service.view.ClaimDetailView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimSubmissionServiceTest {

    /** 21:00 UTC on 14 November is already 08:00 on 15 November in Sydney. */
    private static final Instant LATE_EVENING_UTC = Instant.parse("2026-11-14T21:00:00Z");

    @Mock
    private ClaimRepository claims;
    @Mock
    private PolicyRepository policies;
    @Mock
    private UserRepository users;
    @Mock
    private ClaimEventPublisher events;

    private ClaimSubmissionService submission;

    private final AppUser claimant = claimant();
    private final Policy policy = motorPolicy(claimant);

    @BeforeEach
    void setUp() {
        submission = new ClaimSubmissionService(claims, policies, users, events,
                Clock.fixed(LATE_EVENING_UTC, ZoneOffset.UTC));
        when(users.findById(claimant.getId())).thenReturn(Optional.of(claimant));
    }

    @Test
    void aSubmittedClaimGetsANumberIsSavedAndIsAnnounced() {
        when(policies.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(claims.nextClaimSequence()).thenReturn(42L);

        ClaimDetailView claim = submission.submit(actor(), command(LocalDate.parse("2026-11-10")));

        assertThat(claim.summary().claimNumber()).isEqualTo("CLM-2026-000042");
        assertThat(claim.summary().status()).isEqualTo(ClaimStatus.SUBMITTED);
        verify(claims).save(any());
        verify(events).publish(any());
    }

    @Test
    void theClaimantIsNotShownInternalFigures() {
        when(policies.findById(policy.getId())).thenReturn(Optional.of(policy));

        ClaimDetailView claim = submission.submit(actor(), command(LocalDate.parse("2026-11-10")));

        assertThat(claim.reserveAmount()).isNull();
        assertThat(claim.sumInsured()).isNull();
    }

    @Test
    void anIncidentThisMorningInSydneyIsAcceptedWhileUtcIsStillOnTheDayBefore() {
        when(policies.findById(policy.getId())).thenReturn(Optional.of(policy));

        ClaimDetailView claim = submission.submit(actor(), command(LocalDate.parse("2026-11-15")));

        assertThat(claim.summary().incidentDate()).isEqualTo("2026-11-15");
    }

    @Test
    void aPolicyHeldBySomeoneElseIsRefusedAndNothingIsSaved() {
        Policy someoneElses = motorPolicy(claimant());
        when(policies.findById(someoneElses.getId())).thenReturn(Optional.of(someoneElses));
        SubmitClaimCommand command = new SubmitClaimCommand(someoneElses.getId(), LocalDate.parse("2026-11-10"),
                null, "Rear-ended at a traffic light", new BigDecimal("900.00"));

        assertThatThrownBy(() -> submission.submit(actor(), command))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(claims, never()).save(any());
        verify(events, never()).publish(any());
    }

    private Actor actor() {
        return new Actor(claimant.getId(), claimant.getRole());
    }

    private SubmitClaimCommand command(LocalDate incidentDate) {
        return new SubmitClaimCommand(policy.getId(), incidentDate, "Orchard Road", "Rear-ended at a traffic light",
                new BigDecimal("900.00"));
    }
}
