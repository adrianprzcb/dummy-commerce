package com.adrianperezcobo.dummycommerce.notifications.user.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_contacts")
public class UserContactJpaEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserContactJpaEntity() { }
    public String getEmail() { return email; }
}
