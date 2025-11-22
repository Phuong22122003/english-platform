package com.english.content_service.dto.request;

import com.english.content_service.entity.Options;
import com.english.enums.RequestType;
import lombok.Data;


@Data
public class ToeicTestQuestionRequest {
    private String id;
    private String question;
    private Options options;
    private String correctAnswer;
    private String explanation;
    private Integer part;
    private RequestType action;
    private String imageName;
    private String audioName;
}
