package com.english.content_service.dto.request;

import com.english.enums.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarTopicRequest {
    private String name;
    private String description;
    private Level level;
}
