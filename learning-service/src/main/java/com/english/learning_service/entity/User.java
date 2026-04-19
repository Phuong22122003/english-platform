package com.english.learning_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    private String id;

    @Column(name = "username")
    private String username;

    @Column(name = "avatar_url")
    private String avatarUrl;

//    @Version
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
