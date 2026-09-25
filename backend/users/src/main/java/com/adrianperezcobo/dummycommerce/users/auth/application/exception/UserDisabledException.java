package com.adrianperezcobo.dummycommerce.users.auth.application.exception;

public class UserDisabledException extends RuntimeException {

    public UserDisabledException() {
        super("User account is disabled");
    }
}