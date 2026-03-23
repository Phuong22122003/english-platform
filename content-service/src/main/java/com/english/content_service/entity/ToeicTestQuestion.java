package com.english.content_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "toeic_test_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToeicTestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Liên kết với bảng Group mới theo đúng Database
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "question_group_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_toeic_test_question__toeic_test_question_group__group_id")
    )
    private ToeicTestQuestionGroup questionGroup;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Options options;

    @Column(name = "correct_answer", length = 100, nullable = false)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @CreationTimestamp // Thay cho PrePersist để tự động hóa hoàn toàn
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}