package com.english.content_service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "toeic_test_question_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToeicTestQuestionGroup {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false, foreignKey = @ForeignKey(name = "fk_toeic_test_question_group__toeic_test__test_id"))
    private ToeicTest test;

    @Column(name = "passage_text", columnDefinition = "TEXT")
    private String passageText;

    @Column(name = "image_urls", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> imageUrls;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "public_audio_id", columnDefinition = "TEXT")
    private String publicAudioId;

    @Column(name = "public_image_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> publicImageIds;

    @Column(nullable = false)
    private Integer part;

    @Column(name = "group_order")
    private Integer groupOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Quan hệ 1-N với bảng Question (đã thống nhất ở Database)
    @OneToMany(mappedBy = "questionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<ToeicTestQuestion> questions;

}