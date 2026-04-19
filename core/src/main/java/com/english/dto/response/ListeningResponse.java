package com.english.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListeningResponse {
    private String id;

    private ListeningTopicResponse topic;

    private String name;

    private String audioUrl;

    private String imageUrl;

    private String transcript;

    private String question;

    private Options options;

    private String correctAnswer;

    private LocalDateTime createdAt;
}
