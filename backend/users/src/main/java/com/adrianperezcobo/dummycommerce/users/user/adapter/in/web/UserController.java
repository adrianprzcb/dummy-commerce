package com.adrianperezcobo.dummycommerce.users.user.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.auth.application.result.AuthenticatedUser;
import com.adrianperezcobo.dummycommerce.users.user.adapter.in.web.response.UserResponse;
import com.adrianperezcobo.dummycommerce.users.user.application.port.in.GetCurrentUserUseCase;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UserWebMapper mapper;

    public UserController(
            GetCurrentUserUseCase getCurrentUserUseCase,
            UserWebMapper mapper
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/me")
    public UserResponse getMe(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {
        User user =
                getCurrentUserUseCase.getCurrentUser(
                        authenticatedUser.userId()
                );

        return mapper.toResponse(user);
    }
}