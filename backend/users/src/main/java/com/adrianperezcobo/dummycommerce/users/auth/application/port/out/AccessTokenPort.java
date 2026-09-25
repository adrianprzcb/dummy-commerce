package com.adrianperezcobo.dummycommerce.users.auth.application.port.out;

import com.adrianperezcobo.dummycommerce.users.auth.application.result.AuthenticatedUser;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;

public interface AccessTokenPort {

    String generate(User user);

    AuthenticatedUser parse(String token);

    long getExpirationSeconds();
}