package com.chubb.claims.infrastructure.persistence;

import com.chubb.claims.domain.model.ExposureKey;
import com.chubb.claims.domain.model.ExposureSummary;
import com.chubb.claims.domain.repository.ExposureSummaryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface JpaExposureSummaryRepository extends Repository<ExposureSummary, ExposureKey>, ExposureSummaryRepository {

    @Override
    default void applyDelta(ExposureKey key, long openClaimDelta, BigDecimal outstandingDelta, Instant at) {
        upsert(key.countryCode(), key.lineOfBusiness().name(), key.currency(), openClaimDelta, outstandingDelta, at);
    }

    /**
     * A single atomic statement, so two consumer threads updating the same row cannot lose an update and the
     * first event for a new market creates its row.
     */
    @Modifying
    @Query(value = """
            INSERT INTO exposure_summary
                (country_code, line_of_business, currency, open_claim_count, outstanding_amount, updated_at)
            VALUES (:countryCode, :lineOfBusiness, :currency, :openClaimDelta, :outstandingDelta, :at)
            ON CONFLICT (country_code, line_of_business, currency) DO UPDATE
            SET open_claim_count   = exposure_summary.open_claim_count + EXCLUDED.open_claim_count,
                outstanding_amount = exposure_summary.outstanding_amount + EXCLUDED.outstanding_amount,
                updated_at         = GREATEST(exposure_summary.updated_at, EXCLUDED.updated_at)
            """, nativeQuery = true)
    void upsert(@Param("countryCode") String countryCode,
                @Param("lineOfBusiness") String lineOfBusiness,
                @Param("currency") String currency,
                @Param("openClaimDelta") long openClaimDelta,
                @Param("outstandingDelta") BigDecimal outstandingDelta,
                @Param("at") Instant at);
}
