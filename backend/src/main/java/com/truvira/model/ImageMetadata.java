package com.truvira.model;

/**
 * RECORD = a compact, IMMUTABLE data class (Java 16+).
 * Writing "record ImageMetadata(String cameraMake, ...)" makes Java generate for us:
 *   - private final fields, a constructor taking all of them,
 *   - "getter" methods named like the fields: cameraMake(), cameraModel() ...
 *   - equals(), hashCode() and toString().
 * Immutable = once created it cannot change. Good for evidence: what we read stays what we read.
 *
 * It is NOT an @Entity (no table). It only carries the result from MetadataService
 * to EvidenceService, which copies the values into the Evidence row.
 *
 * Wrapper types (Integer, Double) instead of int/double because they can be null = "not present".
 */
public record ImageMetadata(
        boolean exifFound,     // did the file contain ANY EXIF block at all?
        String cameraMake,     // e.g. "Apple", "samsung"
        String cameraModel,    // e.g. "iPhone 13"
        String software,       // program that wrote the file, e.g. "Adobe Photoshop 25.0"
        String takenAt,        // EXIF "DateTimeOriginal" as written by the camera, e.g. "2026:08:12 18:02:47"
        Integer imageWidth,    // pixels
        Integer imageHeight,   // pixels
        Double gpsLatitude,    // decimal degrees, null if no GPS
        Double gpsLongitude
) {
    /** Factory method for "we could not read anything" (static = called on the type, not an object). */
    public static ImageMetadata empty() {
        return new ImageMetadata(false, null, null, null, null, null, null, null, null);
    }
}
