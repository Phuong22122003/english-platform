package com.english.content_service.dto.request;

import com.english.enums.Level;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicRequest {
    private String id;
    private String name;
    private String description;
    private String topic_type;
    private Level level;
}
