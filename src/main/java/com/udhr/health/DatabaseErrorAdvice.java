package com.udhr.health;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Database errors that slip past a controller become a plain message, never
 * SQL, table names or a stack trace (those help an attacker, not the user).
 * The details still go to the server log for the developer.
 */
@RestControllerAdvice
public class DatabaseErrorAdvice {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DatabaseErrorAdvice.class);
    private final DatabaseHealth health;

    public DatabaseErrorAdvice(DatabaseHealth health) {
        this.health = health;
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<?> databaseDown(Exception e) {
        LOG.error("Database unreachable", e);
        health.checkNow();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("databaseUnavailable", true, "message", DatabaseGuardFilter.MESSAGE));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> badData(DataIntegrityViolationException e) {
        LOG.warn("Rejected by the database: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(Map.of("message",
                "That couldn't be saved: a value is too long, missing, or already used. Check the form and try again."));
    }
}
