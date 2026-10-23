package com.truvira.dto;

import com.truvira.model.CaseStatus;
import com.truvira.model.InvestigationCase;

import java.time.LocalDateTime;

/**
 * RESPONSE DTO: what the API sends back for a case.
 * It has an extra field the entity doesn't store (evidenceCount) and no list of evidence,
 * so the JSON stays small and can never loop.
 */
public record CaseSummary(
        Long id,
        String title,
        String description,
        CaseStatus status,
        LocalDateTime createdAt,
        long evidenceCount
) {
    /** Factory method: build the DTO from an entity + a count. "static" = called as CaseSummary.from(...). */
    public static CaseSummary from(InvestigationCase c, long evidenceCount) {
        return new CaseSummary(c.getId(), c.getTitle(), c.getDescription(),
                c.getStatus(), c.getCreatedAt(), evidenceCount);
    }
}
