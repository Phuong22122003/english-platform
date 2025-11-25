package com.english.learning_service.httpclient;

import com.english.dto.response.ListeningTestReponse;
import com.english.dto.response.ToeicTestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "toeic",url = "${app.services.content}/toeic")
public interface ToeicClient {
    @GetMapping("/tests")
    public List<ToeicTestResponse> getTestsByIds(@RequestParam(name = "ids") List<String> ids);
    @GetMapping("/tests/{test_id}")
    public ToeicTestResponse getTestDetail(@PathVariable("test_id") String testId);
    @PatchMapping("/tests/{test_id}")
    public ResponseEntity<String> updateTotalCompletion(@PathVariable("test_id") String testId);
}
