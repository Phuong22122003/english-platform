package com.english.content_service.entity;

import com.english.enums.Level;
import com.english.utilities.Slug;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "listening_topic")
public class ListeningTopic {
    @Id
    private String id;

    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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