package com.adrianperezcobo.dummycommerce.users.user.adapter.in.web;

import com.adrianperezcobo.dummycommerce.users.user.adapter.in.web.response.UserResponse;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserWebMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}