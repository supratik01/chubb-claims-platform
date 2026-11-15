package com.chubb.claims.domain.repository;

import com.chubb.claims.domain.model.ExposureKey;
import com.chubb.claims.domain.model.ExposureSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ExposureSummaryRepository {

    /** Adds the given differences to the row for the key, creating the row if it does not exist yet. */
    void applyDelta(ExposureKey key, long openClaimDelta, BigDecimal outstandingDelta, Instant at);

    List<ExposureSummary> findAllByOrderByKeyCountryCodeAscKeyLineOfBusinessAsc();
}
