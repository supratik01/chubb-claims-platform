package com.chubb.claims.api.dto;

import com.chubb.claims.service.view.ClaimSummaryView;
import java.util.List;
import org.springframework.data.domain.Page;

public record ClaimPageResponse(List<ClaimSummaryResponse> items, int page, int size, long totalItems,
                                int totalPages) {

    public static ClaimPageResponse from(Page<ClaimSummaryView> page) {
        return new ClaimPageResponse(page.getContent().stream().map(ClaimSummaryResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
