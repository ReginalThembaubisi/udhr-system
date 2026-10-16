package com.udhr.exception;

/** Sign-in refused because of too many recent failures; answered with 429. */
public class LoginLockedException extends RuntimeException {
    public LoginLockedException(String message) {
        super(message);
    }
}
