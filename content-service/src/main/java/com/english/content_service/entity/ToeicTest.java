package com.english.content_service.entity;

import com.english.utilities.Slug;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "toeic_test")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToeicTest {

    @Id
    private String id;

    private String name;

    @Column(name = "total_completion")
    private Integer totalCompletion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ToeicTestGroup group;

//    @Column(name = "part_audios", columnDefinition = "jsonb")
//    @JdbcTypeCode(SqlTypes.JSON)
//    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
//    private List<String> partAudios;


    @PrePersist
    public void ensureId() {
        if (this.id == null || this.id.isEmpty()) {
            String randomNum = String.valueOf((int) (Math.random() * 1001));
            this.id = Slug.generate(this.name) + randomNum;
        }
    }
}
