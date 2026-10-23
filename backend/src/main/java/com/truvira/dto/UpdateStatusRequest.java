package com.truvira.dto;

import com.truvira.model.CaseStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body for PATCH /api/cases/{id}/status:  {"status": "UNDER_REVIEW"}
 * Jackson converts the JSON text "UNDER_REVIEW" into the enum value CaseStatus.UNDER_REVIEW.
 * An unknown word like "DONE" fails conversion -> 400 Bad Request before our code even runs.
 */
public record UpdateStatusRequest(
        @NotNull(message = "Status is required")
        CaseStatus status
) {
}
