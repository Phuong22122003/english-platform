package com.english.content_service.dto.request;

import com.english.enums.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GrammarRequest {
    private String title;
    private Level level;
    private String content;
}
