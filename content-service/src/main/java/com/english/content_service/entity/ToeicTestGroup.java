package com.english.content_service.entity;

import com.english.utilities.Slug;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "toeic_test_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToeicTestGroup {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void ensureId() {
        if (this.id == null || this.id.isEmpty()) {
            String randomNum = String.valueOf((int) (Math.random() * 1001));
            this.id = Slug.generate(this.name) + randomNum;
        }
    }
}
