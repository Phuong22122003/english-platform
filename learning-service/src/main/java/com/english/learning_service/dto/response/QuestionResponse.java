package com.english.learning_service.dto.response;

import com.english.dto.response.Options;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponse {
    private String question;
    private Options options;
    private String correctAnswer;
    private Integer questionOrder;
    private String audioUrl;
    private String imageUrl;
    private String userAnswer;
    private String explanation;
    private Integer part;
}
