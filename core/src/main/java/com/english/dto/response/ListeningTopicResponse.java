package com.english.dto.response;

import com.english.enums.Level;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListeningTopicResponse {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private Level level;
    private LocalDateTime createdAt;
    private List<ListeningResponse> listenings;
    private Page<ListeningTestReponse> tests;
}
