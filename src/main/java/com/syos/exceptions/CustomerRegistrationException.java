package com.syos.exceptions;

public class CustomerRegistrationException extends RuntimeException {
    public CustomerRegistrationException(String message) {
        super(message);
    }

    public CustomerRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}