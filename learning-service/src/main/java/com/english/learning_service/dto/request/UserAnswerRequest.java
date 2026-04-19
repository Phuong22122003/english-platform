package com.english.learning_service.dto.request;

import com.english.dto.response.Options;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnswerRequest {
    @NotNull(message = "Question cannot null")
    private String question;
    private String selectedAnswer;
    @NotNull(message = "Correct answer cannot null")
    private String correctAnswer;
    @NotNull(message = "Options cannot null")
    private Options options;
    private String explanation;
}
