package com.english.content_service.entity;

import com.english.enums.Level;
import com.english.utilities.Slug;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "grammar")
public class Grammar {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private GrammarTopic topic;

    private String title;

    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Level level;

    @PrePersist
    public void ensureId() {
        if (this.id == null || this.id.isEmpty()) {
            String randomNum = String.valueOf((int) (Math.random() * 1001));
            this.id = Slug.generate(this.title) + randomNum;
        }
    }
}