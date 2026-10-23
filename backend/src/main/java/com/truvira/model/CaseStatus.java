package com.truvira.model;

/**
 * ENUM = a type with a FIXED list of allowed values.
 * A case can only ever be one of these three; "CLOSD" (a typo) would not even compile.
 * Compare with a plain String status, where any spelling mistake would slip into the database.
 */
public enum CaseStatus {
    OPEN,           // evidence still being collected
    UNDER_REVIEW,   // a human reviewer is examining the evidence
    CLOSED          // review finished
}
