package com.chubb.claims.support;

import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.domain.model.Policy;
import com.chubb.claims.domain.model.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/** Plain-object builders shared by the unit tests. Nothing here touches Spring or a database. */
public final class Fixtures {

    public static final Instant NOW = Instant.parse("2026-11-15T09:00:00Z");
    public static final LocalDate TODAY = NOW.atZone(ZoneOffset.UTC).toLocalDate();
    public static final BigDecimal SUM_INSURED = new BigDecimal("50000.00");

    private Fixtures() {
    }

    public static AppUser claimant() {
        return user("Mei Ling Tan", Role.CLAIMANT);
    }

    public static AppUser officer() {
        return user("Sam Lee", Role.CLAIMS_OFFICER);
    }

    public static AppUser manager() {
        return user("Kenji Sato", Role.CLAIMS_MANAGER);
    }

    public static AppUser user(String displayName, Role role) {
        return new AppUser(UUID.randomUUID(), displayName.toLowerCase().replace(' ', '.'), "not-a-real-hash",
                displayName, role);
    }

    public static Policy motorPolicy(AppUser holder) {
        return new Policy(UUID.randomUUID(), "POL-SG-MOTOR-1", holder, LineOfBusiness.MOTOR, "SG", "SGD", SUM_INSURED,
                TODAY.minusMonths(6), TODAY.plusMonths(6));
    }

    public static Claim submittedClaim(AppUser claimant, String claimedAmount) {
        return Claim.submit("CLM-2026-000001", motorPolicy(claimant), claimant, TODAY.minusDays(2), "Orchard Road",
                "Rear-ended at a traffic light", new BigDecimal(claimedAmount), NOW, TODAY);
    }

    public static Claim claimUnderReview(AppUser claimant, AppUser officer, String claimedAmount) {
        Claim claim = submittedClaim(claimant, claimedAmount);
        claim.pickUp(officer, NOW);
        claim.pullEvents();
        return claim;
    }
}
