package com.english.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VocabularyTestQuestionResponse {
    private String id;
    private String testId;
    private String question;
    private Options options;
    private String correctAnswer;
    private Integer questionOrder;
    private String imageUrl;
    private String explanation;
}
