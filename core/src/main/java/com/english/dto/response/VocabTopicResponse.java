package com.english.dto.response;

import com.english.enums.Level;
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
public class VocabTopicResponse {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String createdAt;
    private Level level;
    List<VocabularyResponse> vocabularies;
}
