package com.english.learning_service.dto.response;

import com.english.dto.response.Options;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VocabularyTestQuestionResponse {
    private String id;
    private String testId;
    private String question;
    private String correctAnswer;
    private Integer questionOrder;
    private String imageUrl;
    private String explanation;
    private Options options;
}
