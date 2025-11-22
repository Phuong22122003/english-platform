package com.english.content_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToeicTestRequest {
    private String name;
    private List<ToeicTestQuestionRequest> questions;
}
