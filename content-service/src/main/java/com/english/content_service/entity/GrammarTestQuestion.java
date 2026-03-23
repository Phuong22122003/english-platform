package com.english.content_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "grammar_test_question")
public class GrammarTestQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "test_id")
    private GrammarTest test;

    private String question;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Options options;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "question_order")
    private Integer questionOrder;

    private String explanation;
}