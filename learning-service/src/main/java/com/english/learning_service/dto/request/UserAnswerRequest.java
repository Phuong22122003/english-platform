package com.english.learning_service.dto.request;

import com.english.dto.response.Options;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnswerRequest {
    private String question;
    private String selectedAnswer;
    private String correctAnswer;
    private Options options;
    private String explanation;
}
