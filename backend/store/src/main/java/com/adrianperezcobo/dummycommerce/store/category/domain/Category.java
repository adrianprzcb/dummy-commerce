package com.adrianperezcobo.dummycommerce.store.category.domain;

import java.util.UUID;

public class Category {

    private final UUID id;
    private String name;

    public Category(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}