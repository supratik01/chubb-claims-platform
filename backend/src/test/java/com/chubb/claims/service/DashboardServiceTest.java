package com.chubb.claims.service;

import static com.chubb.claims.support.Fixtures.NOW;
import static com.chubb.claims.support.Fixtures.officer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.ExposureKey;
import com.chubb.claims.domain.model.ExposureSummary;
import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.domain.model.OfficerStatusCount;
import com.chubb.claims.domain.model.Role;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.domain.repository.ExposureSummaryRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.view.ExposureView;
import com.chubb.claims.service.view.WorkloadView;
import com.chubb.claims.service.view.WorkloadView.OfficerWorkloadView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ClaimRepository claims;
    @Mock
    private UserRepository users;
    @Mock
    private ExposureSummaryRepository exposure;

    private DashboardService dashboard;

    @BeforeEach
    void setUp() {
        dashboard = new DashboardService(claims, users, exposure);
    }

    @Test
    void workloadCountsEachOfficersClaimsByWhoTheyAreWaitingOn() {
        AppUser busy = officer();
        AppUser idle = officer();
        when(users.findByRoleOrderByDisplayName(Role.CLAIMS_OFFICER)).thenReturn(List.of(busy, idle));
        when(claims.countByStatus(ClaimStatus.SUBMITTED)).thenReturn(7L);
        when(claims.countAssignedClaimsByOfficerAndStatus()).thenReturn(List.of(
                count(busy, ClaimStatus.UNDER_REVIEW, 3),
                count(busy, ClaimStatus.INFO_REQUESTED, 2),
                count(busy, ClaimStatus.APPROVED, 1),
                count(busy, ClaimStatus.REJECTED, 4),
                count(busy, ClaimStatus.SETTLED, 5)));

        WorkloadView workload = dashboard.workload();

        assertThat(workload.unassignedCount()).isEqualTo(7);
        assertThat(workload.officers()).containsExactly(
                new OfficerWorkloadView(busy.getId(), busy.getDisplayName(), 3, 2, 1, 9),
                new OfficerWorkloadView(idle.getId(), idle.getDisplayName(), 0, 0, 0, 0));
    }

    @Test
    void exposureIsTotalledPerCurrencyAndNeverAcrossCurrencies() {
        List<ExposureSummary> rows = List.of(
                row("AU", LineOfBusiness.MOTOR, "AUD", 1, "7800.00"),
                row("SG", LineOfBusiness.MOTOR, "SGD", 2, "4200.00"),
                row("SG", LineOfBusiness.PROPERTY, "SGD", 1, "18500.00"));
        when(exposure.findAllByOrderByKeyCountryCodeAscKeyLineOfBusinessAsc()).thenReturn(rows);

        ExposureView view = dashboard.exposure();

        assertThat(view.breakdown()).hasSize(3);
        assertThat(view.totals()).containsExactly(
                new ExposureView.CurrencyTotal("AUD", 1, new BigDecimal("7800.00")),
                new ExposureView.CurrencyTotal("SGD", 3, new BigDecimal("22700.00")));
    }

    private static OfficerStatusCount count(AppUser officer, ClaimStatus status, long claimCount) {
        return new OfficerStatusCount(officer.getId(), officer.getDisplayName(), status, claimCount);
    }

    private static ExposureSummary row(String country, LineOfBusiness line, String currency, long openClaims,
                                       String outstanding) {
        ExposureSummary row = mock(ExposureSummary.class);
        when(row.getKey()).thenReturn(new ExposureKey(country, line, currency));
        when(row.getOpenClaimCount()).thenReturn(openClaims);
        when(row.getOutstandingAmount()).thenReturn(new BigDecimal(outstanding));
        when(row.getUpdatedAt()).thenReturn(NOW);
        return row;
    }
}
