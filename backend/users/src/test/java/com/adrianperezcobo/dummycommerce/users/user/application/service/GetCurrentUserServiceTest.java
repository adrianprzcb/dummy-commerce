package com.adrianperezcobo.dummycommerce.users.user.application.service;

import com.adrianperezcobo.dummycommerce.users.user.application.exception.UserNotFoundException;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserService service;

    @Test
    void shouldReturnCurrentUser() {
        User user = new User(
                UUID.randomUUID(),
                "adrian@example.com",
                "HASH",
                Role.USER,
                true,
                Instant.now()
        );

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        User result =
                service.getCurrentUser(user.getId());

        assertThat(result).isSameAs(user);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getCurrentUser(userId)
        ).isInstanceOf(UserNotFoundException.class);
    }
}