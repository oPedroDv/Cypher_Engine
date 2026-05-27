package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidNFeException extends CypherException {

    public InvalidNFeException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_NFE");
    }

    public InvalidNFeException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST, "INVALID_NFE");
    }
}
