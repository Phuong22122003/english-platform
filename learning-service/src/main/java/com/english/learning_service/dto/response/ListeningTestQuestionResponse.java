package com.english.learning_service.dto.response;

import com.english.dto.response.Options;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListeningTestQuestionResponse {
    private String id;
    private com.english.dto.response.ListeningTestQuestionResponse test;
    private String audioUrl;
    private String imageUrl;
    private String question;
    private Options options;
    private String correctAnswer;
    private String explanation;
    private Integer questionOrder;
}
