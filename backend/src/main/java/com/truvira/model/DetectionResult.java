package com.truvira.model;

import java.time.LocalDateTime;

/**
 * The answer from DetectorClient, passed to Evidence.applyDetection(...).
 * A record (like ImageMetadata): just data, cannot change after it's made.
 */
public record DetectionResult(
        DetectionStatus status,
        Double aiScore,        // 0.0 (looks real) ... 1.0 (looks AI-generated); null unless COMPLETED
        String model,          // which model answered, e.g. "haywoodsloan/ai-image-detector-deploy"
        String message,        // short explanation when it didn't complete
        LocalDateTime checkedAt
) {
    /** "static factory methods": readable shortcuts for the three outcomes. */
    public static DetectionResult completed(double score, String model) {
        return new DetectionResult(DetectionStatus.COMPLETED, score, model, null, LocalDateTime.now());
    }

    public static DetectionResult unavailable(String message) {
        return new DetectionResult(DetectionStatus.UNAVAILABLE, null, null, message, LocalDateTime.now());
    }

    public static DetectionResult failed(String message) {
        return new DetectionResult(DetectionStatus.FAILED, null, null, message, LocalDateTime.now());
    }
}
