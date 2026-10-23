package com.truvira.dto;

import com.truvira.model.ReviewConclusion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for PUT /api/evidence/{id}/review:
 *   {"conclusion": "LIKELY_AI_GENERATED", "note": "AI score 98%, no EXIF..."}
 * Only these two boxes are on the "form". The server adds the rest itself:
 *   - the reviewer's name comes from the LOGIN (Slice 7), so it can't be faked;
 *   - the time comes from the server clock (see Evidence.applyReview).
 */
public record ReviewRequest(
        @NotNull(message = "Choose a conclusion")
        ReviewConclusion conclusion,

        @Size(max = 2000, message = "Note must be at most 2000 characters")
        String note
) {
}
