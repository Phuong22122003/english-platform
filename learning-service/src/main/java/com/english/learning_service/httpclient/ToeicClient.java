package com.english.learning_service.httpclient;

import com.english.dto.response.ToeicTestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "toeic",url = "${app.services.content}/toeic")
public interface ToeicClient {
    @GetMapping("/tests")
    public List<ToeicTestResponse> getTestsByIds(@RequestParam(name = "ids") List<String> ids);
    @GetMapping("/tests/{test_id}")
    public ToeicTestResponse getTestDetail(@PathVariable("test_id") String testId);
    @PutMapping("/tests/{test_id}")
    public ResponseEntity<String> updateTotalCompletion(@PathVariable("test_id") String testId);
}
