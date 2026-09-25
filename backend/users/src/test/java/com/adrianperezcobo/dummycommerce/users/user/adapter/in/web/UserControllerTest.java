package com.adrianperezcobo.dummycommerce.users.user.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.security.JwtAuthenticationFilter;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.security.SecurityConfig;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.AuthenticatedUser;
import com.adrianperezcobo.dummycommerce.users.shared.adapter.in.web.GlobalExceptionHandler;
import com.adrianperezcobo.dummycommerce.users.user.application.port.in.GetCurrentUserUseCase;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({
        UserWebMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @MockitoBean
    private AccessTokenPort accessTokenPort;

    @Test
    void shouldRejectRequestWithoutJwt()
            throws Exception {

        mockMvc.perform(
                        get("/api/users/me")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserWithValidJwt()
            throws Exception {

        UUID userId = UUID.randomUUID();

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        userId,
                        "adrian@example.com",
                        Role.USER
                );

        User user = new User(
                userId,
                "adrian@example.com",
                "HASH",
                Role.USER,
                true,
                Instant.now()
        );

        when(accessTokenPort.parse("VALID_TOKEN"))
                .thenReturn(authenticatedUser);

        when(getCurrentUserUseCase
                .getCurrentUser(userId))
                .thenReturn(user);

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer VALID_TOKEN"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("adrian@example.com"))
                .andExpect(jsonPath("$.role")
                        .value("USER"))
                .andExpect(jsonPath("$.enabled")
                        .value(true));
    }
}