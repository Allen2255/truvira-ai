package com.truvira.service;

/**
 * A CUSTOM EXCEPTION for "the record you asked for does not exist".
 * extends RuntimeException = an UNCHECKED exception: methods don't need "throws NotFoundException".
 *
 * Having our own type lets the error handler map it to HTTP 404 Not Found,
 * while IllegalArgumentException (bad input) stays 400 Bad Request.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message); // pass the message up to RuntimeException, so getMessage() returns it
    }
}
