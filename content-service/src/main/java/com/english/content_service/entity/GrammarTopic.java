package com.english.content_service.entity;

import com.english.enums.Level;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "grammar_topic")
public class GrammarTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private Level level;
}