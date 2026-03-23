package com.english.content_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "listening_test_question")
public class ListeningTestQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "test_id")
    private ListeningTest test;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "image_url")
    private String imageUrl;

    private String question;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Options options;

    @Column(name = "correct_answer")
    private String correctAnswer;

    private String explanation;

    @Column(name = "public_audio_id")
    private String publicAudioId;

    @Column(name = "public_image_id")
    private String publicImageId;

    @Column(name = "question_order")
    private Integer questionOrder;
}