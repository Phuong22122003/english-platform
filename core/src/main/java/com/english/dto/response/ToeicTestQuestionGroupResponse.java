package com.english.dto.response;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
@Data
public class ToeicTestQuestionGroupResponse {
    private String id;
    private String passageText;
    private List<String> imageUrls;
    private String audioUrl;
    private Integer part;
    private LocalDateTime createdAt;
    private List<ToeicTestQuestionResponse> questions;
}
