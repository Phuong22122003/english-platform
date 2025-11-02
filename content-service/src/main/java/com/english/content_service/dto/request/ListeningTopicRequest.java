package com.english.content_service.dto.request;


import com.english.enums.Level;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListeningTopicRequest {
    private String name;
    private String description;
    private Level level;
}
