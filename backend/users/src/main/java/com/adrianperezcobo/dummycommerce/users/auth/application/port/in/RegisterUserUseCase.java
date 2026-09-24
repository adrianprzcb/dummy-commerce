package com.adrianperezcobo.dummycommerce.users.auth.application.port.in;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);
}