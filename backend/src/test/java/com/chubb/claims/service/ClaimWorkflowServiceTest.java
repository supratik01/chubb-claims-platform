package com.chubb.claims.service;

import static com.chubb.claims.support.Fixtures.NOW;
import static com.chubb.claims.support.Fixtures.claimUnderReview;
import static com.chubb.claims.support.Fixtures.claimant;
import static com.chubb.claims.support.Fixtures.manager;
import static com.chubb.claims.support.Fixtures.officer;
import static com.chubb.claims.support.Fixtures.submittedClaim;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chubb.claims.domain.event.ClaimEventPublisher;
import com.chubb.claims.domain.event.ClaimStatusChanged;
import com.chubb.claims.domain.exception.OperationNotPermittedException;
import com.chubb.claims.domain.exception.ResourceNotFoundException;
import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.view.ClaimDetailView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimWorkflowServiceTest {

    @Mock
    private ClaimRepository claims;
    @Mock
    private UserRepository users;
    @Mock
    private ClaimEventPublisher events;

    private ClaimWorkflowService workflow;

    private final AppUser claimant = claimant();
    private final AppUser assignedOfficer = officer();

    @BeforeEach
    void setUp() {
        workflow = new ClaimWorkflowService(claims, users, events, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void pickingUpAClaimPublishesTheStatusChangeAndSavesTheClaim() {
        Claim claim = stored(submittedClaim(claimant, "4200.00"));
        claim.pullEvents();

        ClaimDetailView result = workflow.pickUp(actorFor(assignedOfficer), claim.getId());

        assertThat(result.summary().status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(result.summary().assignedOfficerName()).isEqualTo(assignedOfficer.getDisplayName());
        verify(claims).save(claim);
        ArgumentCaptor<ClaimStatusChanged> published = ArgumentCaptor.forClass(ClaimStatusChanged.class);
        verify(events).publish(published.capture());
        assertThat(published.getValue().newStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(published.getValue().changedByUserId()).isEqualTo(assignedOfficer.getId());
    }

    @Test
    void aClaimantCannotPickUpAClaim() {
        Claim claim = stored(submittedClaim(claimant, "4200.00"));

        assertThatThrownBy(() -> workflow.pickUp(actorFor(claimant), claim.getId()))
                .isInstanceOf(OperationNotPermittedException.class);
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    @Test
    void anOfficerCannotDecideAClaimAssignedToAColleague() {
        Claim claim = stored(claimUnderReview(claimant, assignedOfficer, "4200.00"));
        AppUser colleague = officer();

        assertThatThrownBy(() -> workflow.approve(actorFor(colleague), claim.getId(), new BigDecimal("100"), null))
                .isInstanceOf(OperationNotPermittedException.class);
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        verify(events, never()).publish(any());
    }

    @Test
    void aManagerCanDecideAClaimAssignedToAnOfficer() {
        Claim claim = stored(claimUnderReview(claimant, assignedOfficer, "4200.00"));
        AppUser manager = manager();

        workflow.reject(actorFor(manager), claim.getId(), "Outside the cover");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void aClaimantCannotAnswerAQuestionOnSomeoneElsesClaim() {
        Claim claim = stored(claimUnderReview(claimant, assignedOfficer, "4200.00"));
        claim.requestInformation(assignedOfficer, "Please send the repair quote", NOW);
        UUID requestId = claim.getInformationRequests().getFirst().getId();
        AppUser stranger = claimant();

        assertThatThrownBy(() -> workflow.answerInformationRequest(actorFor(stranger), claim.getId(), requestId, "Hi"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.INFO_REQUESTED);
    }

    @Test
    void staffCannotAnswerAQuestionOnBehalfOfTheClaimant() {
        Claim claim = stored(claimUnderReview(claimant, assignedOfficer, "4200.00"));
        claim.requestInformation(assignedOfficer, "Please send the repair quote", NOW);
        UUID requestId = claim.getInformationRequests().getFirst().getId();

        assertThatThrownBy(
                () -> workflow.answerInformationRequest(actorFor(assignedOfficer), claim.getId(), requestId, "Hi"))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void actingOnAnUnknownClaimReportsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(claims.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflow.settle(actorFor(assignedOfficer), unknown))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Claim stored(Claim claim) {
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        return claim;
    }

    private Actor actorFor(AppUser user) {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        return new Actor(user.getId(), user.getRole());
    }
}
