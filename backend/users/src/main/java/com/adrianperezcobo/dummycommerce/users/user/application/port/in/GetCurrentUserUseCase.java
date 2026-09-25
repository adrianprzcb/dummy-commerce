package com.adrianperezcobo.dummycommerce.users.user.application.port.in;

import com.adrianperezcobo.dummycommerce.users.user.domain.User;

import java.util.UUID;

public interface GetCurrentUserUseCase {

    User getCurrentUser(UUID userId);
}