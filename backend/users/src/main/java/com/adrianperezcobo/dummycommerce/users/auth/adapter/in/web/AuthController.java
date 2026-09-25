package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request.LoginRequest;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request.RefreshTokenRequest;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request.RegisterUserRequest;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response.LoginResponse;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response.RegisterUserResponse;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LoginUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LogoutUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RefreshTokenUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.LoginResult;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthWebMapper mapper;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase, RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase,
            AuthWebMapper mapper
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        User user = registerUserUseCase.register(
                mapper.toCommand(request)
        );

        return mapper.toResponse(user);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = loginUseCase.login(
                mapper.toCommand(request)
        );

        return mapper.toResponse(result);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return mapper.toResponse(
                refreshTokenUseCase.refresh(
                        mapper.toCommand(request)
                )
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        logoutUseCase.logout(
                request.refreshToken()
        );
    }
}