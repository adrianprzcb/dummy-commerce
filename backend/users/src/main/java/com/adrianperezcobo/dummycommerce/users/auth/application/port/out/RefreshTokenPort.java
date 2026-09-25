package com.adrianperezcobo.dummycommerce.users.auth.application.port.out;

public interface RefreshTokenPort {

    String generate();

    String hash(String rawToken);

    long getExpirationSeconds();
}