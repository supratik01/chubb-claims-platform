package com.chubb.claims.domain.model;

import static com.chubb.claims.domain.model.ClaimStatus.APPROVED;
import static com.chubb.claims.domain.model.ClaimStatus.INFO_REQUESTED;
import static com.chubb.claims.domain.model.ClaimStatus.REJECTED;
import static com.chubb.claims.domain.model.ClaimStatus.SETTLED;
import static com.chubb.claims.domain.model.ClaimStatus.SUBMITTED;
import static com.chubb.claims.domain.model.ClaimStatus.UNDER_REVIEW;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ClaimStatusTest {

    static Stream<Arguments> allowedMoves() {
        return Stream.of(
                Arguments.of(SUBMITTED, Set.of(UNDER_REVIEW)),
                Arguments.of(UNDER_REVIEW, Set.of(INFO_REQUESTED, APPROVED, REJECTED)),
                Arguments.of(INFO_REQUESTED, Set.of(UNDER_REVIEW)),
                Arguments.of(APPROVED, Set.of(SETTLED)),
                Arguments.of(REJECTED, Set.of()),
                Arguments.of(SETTLED, Set.of()));
    }

    @ParameterizedTest(name = "{0} may move only to {1}")
    @MethodSource("allowedMoves")
    void eachStatusAllowsExactlyTheMovesInTheWorkflow(ClaimStatus from, Set<ClaimStatus> expected) {
        for (ClaimStatus candidate : ClaimStatus.values()) {
            assertThat(from.canTransitionTo(candidate))
                    .as("%s -> %s", from, candidate)
                    .isEqualTo(expected.contains(candidate));
        }
    }

    @ParameterizedTest
    @EnumSource(value = ClaimStatus.class, names = {"REJECTED", "SETTLED"})
    void rejectedAndSettledClaimsAreClosed(ClaimStatus status) {
        assertThat(status.isOpen()).isFalse();
    }

    @Test
    void anApprovedClaimIsStillOpenBecauseItHasNotBeenPaid() {
        assertThat(APPROVED.isOpen()).isTrue();
    }
}
