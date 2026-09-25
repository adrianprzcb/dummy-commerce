package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.security.JwtAuthenticationFilter;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.security.SecurityConfig;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.EmailAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LoginUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LogoutUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RefreshTokenUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.LoginResult;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.RefreshTokenResult;
import com.adrianperezcobo.dummycommerce.users.shared.adapter.in.web.GlobalExceptionHandler;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({
        AuthWebMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @MockitoBean
    private AccessTokenPort accessTokenPort;

    @Test
    void shouldRegisterUser() throws Exception {
        User user = new User(
                UUID.randomUUID(),
                "adrian@example.com",
                "HASH",
                Role.USER,
                true,
                Instant.now()
        );

        when(registerUserUseCase.register(any()))
                .thenReturn(user);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "adrian@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(user.getId().toString()))
                .andExpect(jsonPath("$.email")
                        .value("adrian@example.com"))
                .andExpect(jsonPath("$.role")
                        .value("USER"))
                .andExpect(jsonPath("$.enabled")
                        .value(true));
    }

    @Test
    void shouldRejectInvalidRegisterRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "not-an-email",
                                          "password": "123"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Validation error"))
                .andExpect(jsonPath("$.errors.email")
                        .exists())
                .andExpect(jsonPath("$.errors.password")
                        .exists());
    }

    @Test
    void shouldReturnConflictForExistingEmail()
            throws Exception {

        when(registerUserUseCase.register(any()))
                .thenThrow(
                        new EmailAlreadyExistsException(
                                "adrian@example.com"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "adrian@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Email already exists"));
    }

    @Test
    void shouldLogin() throws Exception {
        when(loginUseCase.login(any()))
                .thenReturn(
                        new LoginResult(
                                "ACCESS_TOKEN",
                                "REFRESH_TOKEN",
                                900L,
                                2_592_000L
                        )
                );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "adrian@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("ACCESS_TOKEN"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("REFRESH_TOKEN"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.accessTokenExpiresIn")
                        .value(900))
                .andExpect(jsonPath("$.refreshTokenExpiresIn")
                        .value(2592000));
    }

    @Test
    void shouldRefreshTokens() throws Exception {
        when(refreshTokenUseCase.refresh(any()))
                .thenReturn(
                        new RefreshTokenResult(
                                "NEW_ACCESS",
                                "NEW_REFRESH",
                                900L,
                                2_592_000L
                        )
                );

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "OLD_REFRESH"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("NEW_ACCESS"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("NEW_REFRESH"));
    }

    @Test
    void shouldLogout() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "REFRESH_TOKEN"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        verify(logoutUseCase)
                .logout("REFRESH_TOKEN");
    }
}