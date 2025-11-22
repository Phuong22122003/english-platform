package com.english.learning_service.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "selected_answer", nullable = false, length = 10)
    private String selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    private Integer part;
}
