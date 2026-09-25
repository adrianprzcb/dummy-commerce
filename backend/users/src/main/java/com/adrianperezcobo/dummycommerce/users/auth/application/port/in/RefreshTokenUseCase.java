package com.adrianperezcobo.dummycommerce.users.auth.application.port.in;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RefreshTokenCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.RefreshTokenResult;

public interface RefreshTokenUseCase {

    RefreshTokenResult refresh(
            RefreshTokenCommand command
    );
}
