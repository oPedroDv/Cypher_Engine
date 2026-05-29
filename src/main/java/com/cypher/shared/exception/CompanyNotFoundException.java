package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

public class CompanyNotFoundException extends CypherException {

    public CompanyNotFoundException(String cnpj) {
        super(
                "Empresa não encontrada para cnpj=" + cnpj,
                HttpStatus.NOT_FOUND,
                "COMPANY_NOT_FOUND"
        );
    }
}