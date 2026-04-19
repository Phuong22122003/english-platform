package com.english.learning_service.entity;

import com.english.learning_service.enums.ItemTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "test_type", nullable = false)
    private ItemTypeEnum testType;

    @Column(name = "test_id", nullable = false)
    private String testId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "reply_id")
    private String replyId;

    @Column(name = "parent_id")
    private String parentId;

    @CreationTimestamp
    @Column(name = "commented_at", updatable = false)
    private LocalDateTime commentedAt;

}