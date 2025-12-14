package com.english.learning_service.entity;

import com.english.learning_service.enums.ItemTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "exam_history")
public class ExamHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "test_type", nullable = false)
    private ItemTypeEnum testType;

    @Column(name = "test_id", nullable = false)
    private String testId;

    private int score;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    private String name;

    private Integer duration;
}