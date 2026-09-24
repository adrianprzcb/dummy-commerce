package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase
    ) {
        this.registerUserUseCase =
                registerUserUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse register(
            @Valid @RequestBody RegisterUserRequest request
    ) {

        User user = registerUserUseCase.register(
                new RegisterUserCommand(
                        request.email(),
                        request.password()
                )
        );

        return new RegisterUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}