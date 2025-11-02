package com.english.content_service.entity;

import com.english.enums.Level;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vocabulary_topic")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "public_id")
    private String publicId;

    private Level level;
}