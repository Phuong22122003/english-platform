package com.english.learning_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnswerRequest {
    private String questionId;
    private String selectedAnswer;
    private boolean isCorrect = true;
    private Integer part;
}
