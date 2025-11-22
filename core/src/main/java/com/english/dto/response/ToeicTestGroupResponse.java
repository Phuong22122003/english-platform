package com.english.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToeicTestGroupResponse {
    private String id;
    private String name;
    private LocalDateTime releaseDate;
    private LocalDateTime createdAt;
    private List<ToeicTestResponse> tests;
}
