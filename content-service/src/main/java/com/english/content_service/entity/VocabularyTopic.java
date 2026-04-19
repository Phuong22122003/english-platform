package com.english.content_service.entity;

import com.english.enums.Level;
import com.english.utilities.Slug;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "vocabulary_topic")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyTopic {
    @Id
    private String id;

    private String name;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "public_id")
    private String publicId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Level level;


    @PrePersist
    public void ensureId() {
        if (this.id == null || this.id.isEmpty()) {
            String randomNum = String.valueOf((int) (Math.random() * 1001));
            this.id = Slug.generate(this.name) + randomNum;
        }
    }

}