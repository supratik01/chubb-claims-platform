package com.chubb.claims.service;

import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.ExposureSummary;
import com.chubb.claims.domain.model.OfficerStatusCount;
import com.chubb.claims.domain.model.Role;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.domain.repository.ExposureSummaryRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.view.ExposureView;
import com.chubb.claims.service.view.ExposureView.CurrencyTotal;
import com.chubb.claims.service.view.ExposureView.ExposureLine;
import com.chubb.claims.service.view.WorkloadView;
import com.chubb.claims.service.view.WorkloadView.OfficerWorkloadView;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ClaimRepository claims;
    private final UserRepository users;
    private final ExposureSummaryRepository exposure;

    public DashboardService(ClaimRepository claims, UserRepository users, ExposureSummaryRepository exposure) {
        this.claims = claims;
        this.users = users;
        this.exposure = exposure;
    }

    /**
     * Workload is read live from the claim table. Every claims officer is listed, including those with no
     * claims, so that spare capacity is visible; a manager appears only if they hold claims.
     */
    @Transactional(readOnly = true)
    public WorkloadView workload() {
        Map<UUID, String> names = new LinkedHashMap<>();
        Map<UUID, Map<ClaimStatus, Long>> countsByOfficer = new LinkedHashMap<>();
        users.findByRoleOrderByDisplayName(Role.CLAIMS_OFFICER).forEach(officer -> {
            names.put(officer.getId(), officer.getDisplayName());
            countsByOfficer.put(officer.getId(), new EnumMap<>(ClaimStatus.class));
        });
        for (OfficerStatusCount count : claims.countAssignedClaimsByOfficerAndStatus()) {
            names.putIfAbsent(count.officerId(), count.officerName());
            countsByOfficer.computeIfAbsent(count.officerId(), id -> new EnumMap<>(ClaimStatus.class))
                    .put(count.status(), count.claimCount());
        }

        List<OfficerWorkloadView> officers = new ArrayList<>();
        countsByOfficer.forEach((officerId, counts) -> officers.add(new OfficerWorkloadView(
                officerId,
                names.get(officerId),
                counts.getOrDefault(ClaimStatus.UNDER_REVIEW, 0L),
                counts.getOrDefault(ClaimStatus.INFO_REQUESTED, 0L),
                counts.getOrDefault(ClaimStatus.APPROVED, 0L),
                counts.getOrDefault(ClaimStatus.REJECTED, 0L) + counts.getOrDefault(ClaimStatus.SETTLED, 0L))));

        return new WorkloadView(claims.countByStatus(ClaimStatus.SUBMITTED), officers);
    }

    /** Exposure is read from the summary the Kafka consumer maintains. Totals are per currency, never converted. */
    @Transactional(readOnly = true)
    public ExposureView exposure() {
        List<ExposureLine> breakdown = new ArrayList<>();
        Map<String, CurrencyTotal> totals = new TreeMap<>();
        for (ExposureSummary row : exposure.findAllByOrderByKeyCountryCodeAscKeyLineOfBusinessAsc()) {
            String currency = row.getKey().currency();
            breakdown.add(new ExposureLine(row.getKey().countryCode(), row.getKey().lineOfBusiness(), currency,
                    row.getOpenClaimCount(), row.getOutstandingAmount(), row.getUpdatedAt()));
            totals.merge(currency,
                    new CurrencyTotal(currency, row.getOpenClaimCount(), row.getOutstandingAmount()),
                    (left, right) -> new CurrencyTotal(currency, left.openClaims() + right.openClaims(),
                            left.outstandingAmount().add(right.outstandingAmount())));
        }
        return new ExposureView(List.copyOf(totals.values()), breakdown);
    }
}
