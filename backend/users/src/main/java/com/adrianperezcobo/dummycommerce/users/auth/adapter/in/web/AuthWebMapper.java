package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request.LoginRequest;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request.RegisterUserRequest;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response.LoginResponse;
import com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response.RegisterUserResponse;
import com.adrianperezcobo.dummycommerce.users.auth.application.command.LoginUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.LoginResult;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class AuthWebMapper {

    public RegisterUserCommand toCommand(
            RegisterUserRequest request
    ) {
        return new RegisterUserCommand(
                request.email(),
                request.password()
        );
    }

    public RegisterUserResponse toResponse(
            User user
    ) {
        return new RegisterUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    public LoginUserCommand toCommand(
            LoginRequest request
    ) {
        return new LoginUserCommand(
                request.email(),
                request.password()
        );
    }

    public LoginResponse toResponse(
            LoginResult result
    ) {
        return new LoginResponse(
                result.accessToken(),
                "Bearer",
                result.expiresIn()
        );
    }
}