package com.truvira.model;

/**
 * What happened when we asked the Python AI service about an image.
 * (null in the database = never checked, e.g. evidence uploaded before Slice 5.)
 */
public enum DetectionStatus {
    COMPLETED,     // the detector answered; aiScore holds its probability
    UNAVAILABLE,   // the detector was switched off / not reachable -> FALLBACK, no score invented
    FAILED         // the detector answered with an error (e.g. it couldn't read the image)
}
