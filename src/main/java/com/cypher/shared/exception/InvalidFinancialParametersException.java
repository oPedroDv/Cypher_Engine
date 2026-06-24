package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidFinancialParametersException extends CypherException {

    public InvalidFinancialParametersException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FINANCIAL_PARAMETERS");
    }
}
