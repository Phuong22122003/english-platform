package com.english.learning_service.entity;

import com.english.dto.response.Options;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JoinColumn(name = "exam_history_id")
    private ExamHistory examHistory;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "question")
    private String question;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Options options;

    @Column(name = "correct_answer")
    private String correctAnswer;

    private String explanation;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "selected_answer", nullable = false, length = 10)
    private String selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    private Integer part;
}
