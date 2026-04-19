package com.english.content_service.entity;

import com.english.utilities.Slug;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "grammar_test")
public class GrammarTest {
    @Id
    private String id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "grammar_id")
    private Grammar grammar;

    private Integer duration;

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