package com.english.learning_service.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserAnswerGroupResponse {
    private String passageText;
    private List<UserAnswerResponse> answers;
    private List<String> imageUrls;
    private String audioUrl;
    private Integer part;
    private LocalDateTime createdDate;
}
