package com.english.content_service.dto.request;

import com.english.content_service.entity.Options;
import com.english.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyTestQuestionRequest {
    private String id;

    private String question;

    private Options options;

    private String correctAnswer;

    private Integer questionOrder;

    private String explanation;

    private String imageName;

    private RequestType action;
}
