package com.english.content_service.dto.request;

import com.english.content_service.entity.Options;
import com.english.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ListeningTestQuestionRequest {
    private String id;
    private String question;
    private Options options;
    private String correctAnswer;
    private String explanation;
    private String imageName;
    private String audioName;
    private RequestType action;
}
