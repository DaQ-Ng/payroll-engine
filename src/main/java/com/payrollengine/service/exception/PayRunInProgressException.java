package com.payrollengine.service.exception;

/** Thrown when a pay run is requested for a period that another concurrent
 * request is already processing (won the insert race, hasn't committed yet).
 * The client should retry — see the README's concurrency notes for why this
 * narrow window exists and how it'd be closed with a distributed lock. */
public class PayRunInProgressException extends RuntimeException {
    public PayRunInProgressException(String message) {
        super(message);
    }
}
