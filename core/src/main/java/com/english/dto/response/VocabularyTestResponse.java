package com.english.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VocabularyTestResponse {
    private String id;
    private String topicId;
    private String topicName;
    private String name;
    private int duration;
    private String createdAt;
    List<VocabularyTestQuestionResponse> questions;
}
