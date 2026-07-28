package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends CypherException {

    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
    }
}
