package com.adrianperezcobo.dummycommerce.users.user.application.port.out;

import com.adrianperezcobo.dummycommerce.users.user.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}