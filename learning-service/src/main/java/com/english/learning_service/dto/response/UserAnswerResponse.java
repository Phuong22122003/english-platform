package com.english.learning_service.dto.response;


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
public class UserAnswerResponse {
    private String id;
    private String questionId;
    private String selectedAnswer;
    private boolean isCorrect;
    private Integer part;
}
