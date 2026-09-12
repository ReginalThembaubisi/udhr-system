package com.udhr.exception;

/** Thrown for a rejected login attempt (bad credentials, inactive account, etc). */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
