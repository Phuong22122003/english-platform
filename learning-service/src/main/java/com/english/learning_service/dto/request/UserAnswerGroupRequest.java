package com.english.learning_service.dto.request;


import lombok.Data;
import java.util.List;

@Data
public class UserAnswerGroupRequest {
    private List<UserAnswerRequest> answers;
    private String passageText;
    private List<String> imageUrls;
    private String audioUrl;
    private Integer part;
}
