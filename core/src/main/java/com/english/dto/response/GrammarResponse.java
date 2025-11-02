package com.english.dto.response;

import com.english.enums.Level;
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
public class GrammarResponse {
    private String id;
    private String topicId;
    private String title;
    private String content;
    private String createdAt;
    private String description;
    private Level level;
}
