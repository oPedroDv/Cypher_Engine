package com.cypher.infrastructure.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Exige que o principal autenticado (JWT ou API key) possua o scope granular
 * informado, concedido como authority {@code SCOPE_<value>}.
 *
 * Achado 4.1 da auditoria: esta anotação aceitava {@code ROLE_API_CLIENT} como
 * alternativa válida a qualquer scope, e toda API key recebia essa role
 * incondicionalmente — na prática, nenhuma API key era de fato restrita por
 * scope. O bypass foi removido; agora a única forma de passar é possuir o
 * scope exato, seja via JWT (claim "scope") ou via scopes persistidos em
 * {@code api_keys.scopes}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_{value}')")
public @interface RequiresScope {
    String value();
}
