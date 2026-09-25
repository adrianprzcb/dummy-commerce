package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request.LoginRequest;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request.RegisterUserRequest;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response.LoginResponse;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response.RegisterUserResponse;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LoginUseCase;
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
    private final AuthWebMapper mapper;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            AuthWebMapper mapper
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
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
}