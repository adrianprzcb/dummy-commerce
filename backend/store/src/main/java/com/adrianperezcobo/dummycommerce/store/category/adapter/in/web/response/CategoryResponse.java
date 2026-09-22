package com.adrianperezcobo.dummycommerce.store.category.adapter.in.web.response;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name
) {
}