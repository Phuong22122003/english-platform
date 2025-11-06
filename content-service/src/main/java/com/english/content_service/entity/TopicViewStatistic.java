package com.english.content_service.entity;
import com.english.enums.TopicType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "topic_view_statistic")
public class TopicViewStatistic {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "topic_id", nullable = false)
    private String topicId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "topic_type", nullable = false)
    private TopicType topicType;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    @Column(name = "view_hour", nullable = false)
    private Integer viewHour;

    @Column(name = "view_count", nullable = false)
    @ColumnDefault("0")
    private Integer viewCount;
}
