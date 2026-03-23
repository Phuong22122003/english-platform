package com.english.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToeicTestQuestionResponse {
    private String id;
    private String question;
    private Options options;
    private String correctAnswer;
    private String explanation;
    private LocalDateTime createdAt;
}
