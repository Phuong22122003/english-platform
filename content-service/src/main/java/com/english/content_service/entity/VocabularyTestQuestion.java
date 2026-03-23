package com.english.content_service.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vocabulary_test_question")
public class VocabularyTestQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "test_id")
    private VocabularyTest test;

    private String question;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Options options;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "question_order")
    private Integer questionOrder;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "public_id")
    private String publicId;

    private String explanation;
}