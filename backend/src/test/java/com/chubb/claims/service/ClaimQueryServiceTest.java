package com.chubb.claims.service;

import static com.chubb.claims.support.Fixtures.claimUnderReview;
import static com.chubb.claims.support.Fixtures.claimant;
import static com.chubb.claims.support.Fixtures.officer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chubb.claims.domain.exception.ResourceNotFoundException;
import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimSearchCriteria;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.Role;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.service.command.AssignmentFilter;
import com.chubb.claims.service.view.ClaimDetailView;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimQueryServiceTest {

    @Mock
    private ClaimRepository claims;

    private ClaimQueryService queries;

    private final AppUser claimant = claimant();
    private final AppUser officer = officer();

    @BeforeEach
    void setUp() {
        queries = new ClaimQueryService(claims);
        when(claims.search(any(), any())).thenReturn(Page.empty());
    }

    @Test
    void aClaimantSearchIsAlwaysRestrictedToTheirOwnClaimsNewestFirst() {
        queries.search(new Actor(claimant.getId(), Role.CLAIMANT), AssignmentFilter.ALL, Set.of(), 0, 20);

        assertThat(capturedCriteria().claimantId()).isEqualTo(claimant.getId());
        assertThat(capturedPageable().getSort()).isEqualTo(Sort.by("submittedAt").descending());
    }

    @Test
    void theUnassignedQueueListsClaimsNobodyHasPickedUpOldestFirst() {
        queries.search(new Actor(officer.getId(), Role.CLAIMS_OFFICER), AssignmentFilter.UNASSIGNED,
                Set.of(ClaimStatus.SUBMITTED), 0, 20);

        ClaimSearchCriteria criteria = capturedCriteria();
        assertThat(criteria.unassignedOnly()).isTrue();
        assertThat(criteria.claimantId()).isNull();
        assertThat(criteria.statuses()).containsExactly(ClaimStatus.SUBMITTED);
        assertThat(capturedPageable().getSort()).isEqualTo(Sort.by("submittedAt").ascending());
    }

    @Test
    void myClaimsListsOnlyClaimsAssignedToTheCallingOfficer() {
        queries.search(new Actor(officer.getId(), Role.CLAIMS_OFFICER), AssignmentFilter.MINE, null, 0, 20);

        assertThat(capturedCriteria().assignedOfficerId()).isEqualTo(officer.getId());
    }

    @Test
    void staffSeeTheReserveAndSumInsuredButTheClaimantDoesNot() {
        Claim claim = claimUnderReview(claimant, officer, "4200.00");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        ClaimDetailView forStaff = queries.get(new Actor(officer.getId(), Role.CLAIMS_OFFICER), claim.getId());
        ClaimDetailView forClaimant = queries.get(new Actor(claimant.getId(), Role.CLAIMANT), claim.getId());

        assertThat(forStaff.reserveAmount()).isEqualByComparingTo("4200.00");
        assertThat(forStaff.sumInsured()).isNotNull();
        assertThat(forClaimant.reserveAmount()).isNull();
        assertThat(forClaimant.sumInsured()).isNull();
    }

    @Test
    void aClaimantAskingForSomeoneElsesClaimIsToldItDoesNotExist() {
        Claim claim = claimUnderReview(claimant, officer, "4200.00");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        AppUser stranger = claimant();

        assertThatThrownBy(() -> queries.get(new Actor(stranger.getId(), Role.CLAIMANT), claim.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ClaimSearchCriteria capturedCriteria() {
        ArgumentCaptor<ClaimSearchCriteria> criteria = ArgumentCaptor.forClass(ClaimSearchCriteria.class);
        verify(claims).search(criteria.capture(), any());
        return criteria.getValue();
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(claims).search(any(), pageable.capture());
        return pageable.getValue();
    }
}
