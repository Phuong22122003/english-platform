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
    private String imageUrl;
    private String audioUrl;
    private String publicAudioId;
    private String publicImageId;
    private Integer part;
    private LocalDateTime createdAt;
    private List<ToeicTestQuestionResponse> questions;
}
