package com.chubb.claims.domain.model;

import static com.chubb.claims.support.Fixtures.NOW;
import static com.chubb.claims.support.Fixtures.TODAY;
import static com.chubb.claims.support.Fixtures.claimUnderReview;
import static com.chubb.claims.support.Fixtures.claimant;
import static com.chubb.claims.support.Fixtures.motorPolicy;
import static com.chubb.claims.support.Fixtures.officer;
import static com.chubb.claims.support.Fixtures.submittedClaim;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chubb.claims.domain.event.ClaimStatusChanged;
import com.chubb.claims.domain.exception.BusinessRuleViolationException;
import com.chubb.claims.domain.exception.InvalidStatusTransitionException;
import com.chubb.claims.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClaimTest {

    private final AppUser claimant = claimant();
    private final AppUser officer = officer();

    @Nested
    class Submitting {

        @Test
        void aNewClaimIsSubmittedWithTheClaimedAmountOutstanding() {
            Claim claim = submittedClaim(claimant, "4200.00");

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
            assertThat(claim.getCurrency()).isEqualTo("SGD");
            assertThat(claim.outstandingAmount()).isEqualByComparingTo("4200.00");
            assertThat(claim.getAssignedOfficer()).isNull();
        }

        @Test
        void theReserveIsCappedAtTheSumInsured() {
            Claim claim = submittedClaim(claimant, "75000.00");

            assertThat(claim.getReserveAmount()).isEqualByComparingTo("50000.00");
        }

        @Test
        void submittingRecordsTheFirstHistoryEntryAndRaisesAnEvent() {
            Claim claim = submittedClaim(claimant, "4200.00");

            assertThat(claim.getStatusHistory()).singleElement().satisfies(entry -> {
                assertThat(entry.getFromStatus()).isNull();
                assertThat(entry.getToStatus()).isEqualTo(ClaimStatus.SUBMITTED);
                assertThat(entry.getChangedBy()).isSameAs(claimant);
            });
            assertThat(claim.pullEvents()).singleElement().satisfies(event -> {
                assertThat(event.previousStatus()).isNull();
                assertThat(event.newStatus()).isEqualTo(ClaimStatus.SUBMITTED);
                assertThat(event.previousOutstandingAmount()).isEqualByComparingTo("0");
                assertThat(event.newOutstandingAmount()).isEqualByComparingTo("4200.00");
                assertThat(event.countryCode()).isEqualTo("SG");
                assertThat(event.lineOfBusiness()).isEqualTo(LineOfBusiness.MOTOR);
            });
        }

        @Test
        void aClaimAgainstSomeoneElsesPolicyIsRefused() {
            Policy someoneElsesPolicy = motorPolicy(claimant());

            assertThatThrownBy(() -> submit(someoneElsesPolicy, TODAY.minusDays(1), "100.00"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("your own policy");
        }

        @Test
        void anIncidentDatedInTheFutureIsRefused() {
            assertThatThrownBy(() -> submit(motorPolicy(claimant), TODAY.plusDays(1), "100.00"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("future");
        }

        @Test
        void anIncidentOutsideTheCoverPeriodIsRefused() {
            assertThatThrownBy(() -> submit(motorPolicy(claimant), TODAY.minusYears(2), "100.00"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("cover period");
        }

        private Claim submit(Policy policy, LocalDate incidentDate, String amount) {
            return Claim.submit("CLM-2026-000009", policy, claimant, incidentDate, null, "Hail damage to the roof",
                    new BigDecimal(amount), NOW, TODAY);
        }
    }

    @Nested
    class Reviewing {

        @Test
        void pickingUpAssignsTheOfficerAndStartsTheReview() {
            Claim claim = submittedClaim(claimant, "4200.00");

            claim.pickUp(officer, NOW);

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
            assertThat(claim.isAssignedTo(officer.getId())).isTrue();
        }

        @Test
        void aClaimThatIsAlreadyUnderReviewCannotBePickedUpAgain() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            assertThatThrownBy(() -> claim.pickUp(officer(), NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            assertThat(claim.isAssignedTo(officer.getId())).isTrue();
        }

        @Test
        void requestingInformationOpensAQuestionAndWaitsOnTheClaimant() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            claim.requestInformation(officer, "Please send the repair quote", NOW);

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.INFO_REQUESTED);
            assertThat(claim.getInformationRequests()).singleElement().satisfies(request -> {
                assertThat(request.getQuestion()).isEqualTo("Please send the repair quote");
                assertThat(request.isAnswered()).isFalse();
            });
        }

        @Test
        void answeringTheQuestionReturnsTheClaimToReview() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");
            claim.requestInformation(officer, "Please send the repair quote", NOW);
            UUID requestId = claim.getInformationRequests().getFirst().getId();

            claim.answerInformationRequest(requestId, claimant, "The quote is SGD 4,200", NOW);

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
            assertThat(claim.getInformationRequests().getFirst().getResponse()).isEqualTo("The quote is SGD 4,200");
        }

        @Test
        void answeringAnUnknownQuestionIsRefused() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");
            claim.requestInformation(officer, "Please send the repair quote", NOW);

            assertThatThrownBy(() -> claim.answerInformationRequest(UUID.randomUUID(), claimant, "Here it is", NOW))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void answeringWhenNothingWasAskedIsRefused() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            assertThatThrownBy(() -> claim.answerInformationRequest(UUID.randomUUID(), claimant, "Here it is", NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    class Deciding {

        @Test
        void approvingSetsTheOutstandingAmountToTheApprovedAmount() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            claim.approve(officer, new BigDecimal("3800.00"), "Excess deducted", NOW);

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
            assertThat(claim.getApprovedAmount()).isEqualByComparingTo("3800.00");
            assertThat(claim.outstandingAmount()).isEqualByComparingTo("3800.00");
            ClaimStatusChanged event = onlyEvent(claim);
            assertThat(event.previousOutstandingAmount()).isEqualByComparingTo("4200.00");
            assertThat(event.newOutstandingAmount()).isEqualByComparingTo("3800.00");
        }

        @Test
        void approvingMoreThanTheSumInsuredIsRefused() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            assertThatThrownBy(() -> claim.approve(officer, new BigDecimal("50000.01"), null, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("sum insured");
            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        }

        @Test
        void approvingWithoutAnAmountIsRefused() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            assertThatThrownBy(() -> claim.approve(officer, null, null, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        void rejectingRequiresAReason() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            assertThatThrownBy(() -> claim.reject(officer, "  ", NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("reason");
        }

        @Test
        void rejectingLeavesNothingOutstanding() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");

            claim.reject(officer, "Damage predates the policy", NOW);

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
            assertThat(claim.getDecisionReason()).isEqualTo("Damage predates the policy");
            assertThat(claim.outstandingAmount()).isEqualByComparingTo("0");
            assertThat(onlyEvent(claim).newOutstandingAmount()).isEqualByComparingTo("0");
        }

        @Test
        void aClaimCannotBeDecidedBeforeItIsPickedUp() {
            Claim claim = submittedClaim(claimant, "4200.00");

            assertThatThrownBy(() -> claim.approve(officer, new BigDecimal("100.00"), null, NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            assertThatThrownBy(() -> claim.reject(officer, "Not covered", NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void settlingAnApprovedClaimLeavesNothingOutstanding() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");
            claim.approve(officer, new BigDecimal("3800.00"), null, NOW);
            claim.pullEvents();

            claim.settle(officer, NOW);

            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SETTLED);
            assertThat(claim.outstandingAmount()).isEqualByComparingTo("0");
            assertThat(onlyEvent(claim).previousOutstandingAmount()).isEqualByComparingTo("3800.00");
        }

        @Test
        void aRejectedClaimCannotBeSettled() {
            Claim claim = claimUnderReview(claimant, officer, "4200.00");
            claim.reject(officer, "Not covered", NOW);

            assertThatThrownBy(() -> claim.settle(officer, NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    void everyStatusChangeIsAppendedToTheHistoryInOrder() {
        Claim claim = claimUnderReview(claimant, officer, "4200.00");
        claim.requestInformation(officer, "Please send the repair quote", NOW);
        claim.answerInformationRequest(claim.getInformationRequests().getFirst().getId(), claimant, "Attached", NOW);
        claim.approve(officer, new BigDecimal("4200.00"), null, NOW);
        claim.settle(officer, NOW);

        assertThat(claim.getStatusHistory()).extracting(ClaimStatusChange::getToStatus).containsExactly(
                ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.INFO_REQUESTED,
                ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, ClaimStatus.SETTLED);
    }

    private static ClaimStatusChanged onlyEvent(Claim claim) {
        List<ClaimStatusChanged> events = claim.pullEvents();
        assertThat(events).hasSize(1);
        return events.getFirst();
    }
}
