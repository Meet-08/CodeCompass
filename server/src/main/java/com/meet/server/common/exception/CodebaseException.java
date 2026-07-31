package com.meet.server.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CodebaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public CodebaseException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public CodebaseException(String errorCode, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }
}
