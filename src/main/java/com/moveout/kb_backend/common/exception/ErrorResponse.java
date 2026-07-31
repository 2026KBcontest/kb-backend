package com.moveout.kb_backend.common.exception;

import java.time.Instant;

public record ErrorResponse(boolean success, String errorCode, String message, Instant timestamp) {

    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(false, errorCode, message, Instant.now());
    }
}
