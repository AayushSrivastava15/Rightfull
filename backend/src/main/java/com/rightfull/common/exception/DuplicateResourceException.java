package com.rightfull.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an attempt is made to create a resource with a unique constraint violation (e.g. duplicate slug).
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
    }
}

