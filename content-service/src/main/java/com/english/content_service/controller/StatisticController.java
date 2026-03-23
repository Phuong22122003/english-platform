package com.english.content_service.controller;

import com.english.content_service.dto.response.TopicViewSummaryResponse;
import com.english.content_service.service.StatisticService;
import com.english.dto.response.StatisticResponse;
import com.english.enums.TimeRange;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/statistic/topics")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class StatisticController {
    StatisticService statisticService;
    @GetMapping("/views/time-series")
    public ResponseEntity<StatisticResponse> getTopicViewTimeSeries(@RequestParam("time_range") TimeRange timeRange) {
        return ResponseEntity.ok(statisticService.getTopicViewTimeSeries(timeRange));
    }
    @GetMapping("/views/ranking")
    public ResponseEntity<List<TopicViewSummaryResponse>> getTopTopicRanking(@RequestParam("top") Integer top) {
        return ResponseEntity.ok(statisticService.getTopTopicRanking(top));
    }
}
