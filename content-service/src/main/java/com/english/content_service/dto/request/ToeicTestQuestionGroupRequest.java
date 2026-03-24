package com.english.content_service.dto.request;

import com.english.content_service.entity.ToeicTest;
import com.english.content_service.entity.ToeicTestQuestion;
import com.english.enums.RequestType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ToeicTestQuestionGroupRequest {
    private String id;
    private String passageText;
    private String imageName;
    private String audioName;
    private Integer part;
    private RequestType action;
    private List<ToeicTestQuestionRequest> questions;
}
