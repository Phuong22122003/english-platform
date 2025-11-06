package com.english.learning_service.controller;

import com.english.dto.response.StatisticResponse;
import com.english.enums.TimeRange;
import com.english.learning_service.service.StatisticService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistic")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticController {
    StatisticService statisticService;
    @GetMapping("/test-taken")
    public ResponseEntity<StatisticResponse> getNumberOfTakenTest(@RequestParam("time_range") TimeRange timeRange){
        return ResponseEntity.ok().body(statisticService.getNumberOfTestIsTaken(timeRange));
    }

}
