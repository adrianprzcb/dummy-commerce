package com.adrianperezcobo.dummycommerce.users.user.application.service;

import com.adrianperezcobo.dummycommerce.users.user.application.exception.UserNotFoundException;
import com.adrianperezcobo.dummycommerce.users.user.application.port.in.GetCurrentUserUseCase;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetCurrentUserService
        implements GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(userId)
                );
    }
}