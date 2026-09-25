package com.adrianperezcobo.dummycommerce.users.auth.application.exception;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("Invalid access token");
    }
}