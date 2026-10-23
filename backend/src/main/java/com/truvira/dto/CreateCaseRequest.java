package com.truvira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO = "Data Transfer Object": a small class shaped exactly like the JSON that travels over HTTP.
 * This one is the BODY React sends to create a case:  {"title": "...", "description": "..."}
 *
 * Why not receive an InvestigationCase entity directly? Then a client could also send "id",
 * "status" or "createdAt" and overwrite fields it should never control. A DTO only accepts what we allow.
 *
 * VALIDATION annotations (from spring-boot-starter-validation, Jakarta Bean Validation):
 *   @NotBlank = not null, not "" and not just spaces
 *   @Size(max) = length limit (matches the database column length)
 * They are checked when the controller parameter is marked @Valid.
 */
public record CreateCaseRequest(
        @NotBlank(message = "Case title is required")
        @Size(max = 200, message = "Case title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}
