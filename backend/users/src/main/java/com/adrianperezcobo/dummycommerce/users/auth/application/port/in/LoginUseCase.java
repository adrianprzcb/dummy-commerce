package com.adrianperezcobo.dummycommerce.users.auth.application.port.in;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.LoginUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.LoginResult;

public interface LoginUseCase {

    LoginResult login(LoginUserCommand command);
}