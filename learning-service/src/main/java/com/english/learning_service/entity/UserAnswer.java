package com.english.learning_service.entity;

import com.english.dto.response.Options;
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
@Table(name = "user_answer")
public class UserAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_answer_group_id")
    private UserAnswerGroup answerGroup;

    @Column(name = "question")
    private String question;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Options options;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "selected_answer", nullable = false, length = 10)
    private String selectedAnswer;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
