package com.truvira.model;

/**
 * The HUMAN reviewer's conclusion about one piece of evidence.
 * Every option says "likely": Truvira never declares anything 100% fake or guilty.
 */
public enum ReviewConclusion {
    LIKELY_AUTHENTIC,      // looks like a genuine, unedited photo
    LIKELY_AI_GENERATED,   // looks made by an AI image generator
    LIKELY_MANIPULATED,    // a real photo that seems to have been edited
    INCONCLUSIVE           // not enough evidence either way
}
