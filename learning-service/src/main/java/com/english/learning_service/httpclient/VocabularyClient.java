package com.english.learning_service.httpclient;

import com.english.dto.response.VocabTopicResponse;
import com.english.dto.response.VocabularyTestResponse;
import com.english.learning_service.configuration.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "vocabulary",url = "${app.services.content}/vocabulary",  configuration = FeignConfig.class)
public interface VocabularyClient {
    @GetMapping("/topics/ids")
    public List<VocabTopicResponse> getTopicsByIds(@RequestParam("ids") List<String> ids);
    @GetMapping("/tests")
    public List<VocabularyTestResponse> getTestsByIds(@RequestParam(name = "ids") List<String> ids);
    @GetMapping("/tests/{test_id}/questions")
    public VocabularyTestResponse getTestQuestionsByTestId(@PathVariable(name = "test_id") String testId);
}
