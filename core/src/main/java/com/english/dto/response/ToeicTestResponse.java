package com.english.dto.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToeicTestResponse {
    private String id;
    private String name;
    private Integer totalCompletion;
    private LocalDateTime createdAt;
    private List<ToeicTestQuestionResponse> questions;
}
