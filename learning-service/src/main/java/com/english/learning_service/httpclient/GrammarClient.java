package com.english.learning_service.httpclient;

import com.english.dto.response.GrammarTestResponse;
import com.english.dto.response.GrammarTopicResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "grammar",url = "${app.services.content}/grammar")
public interface GrammarClient {
    @GetMapping("/topics/ids")
    public List<GrammarTopicResponse> getTopicsByIds(@RequestParam("ids") List<String> ids);
    @GetMapping("/tests")
    public List<GrammarTestResponse> getTestsByIds(@RequestParam(name = "ids") List<String> ids);
    @GetMapping("/tests/{test_id}/questions")
    public GrammarTestResponse getTestQuestionsByTestId(@PathVariable("test_id") String testId);
}
