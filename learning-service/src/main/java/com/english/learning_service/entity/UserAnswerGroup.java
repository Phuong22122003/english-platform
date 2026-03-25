package com.english.learning_service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "user_answer_group")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class UserAnswerGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "exam_history_id")
    private ExamHistory examHistory;

    @OneToMany(mappedBy = "answerGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAnswer> answers;

    @Column(name = "passage_text", columnDefinition = "TEXT")
    private String passageText;

    @Column(name = "image_urls", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> imageUrls;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "part")
    private Integer part;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
