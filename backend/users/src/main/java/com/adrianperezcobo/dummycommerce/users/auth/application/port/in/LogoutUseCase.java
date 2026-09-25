package com.adrianperezcobo.dummycommerce.users.auth.application.port.in;

public interface LogoutUseCase {

    void logout(String refreshToken);
}