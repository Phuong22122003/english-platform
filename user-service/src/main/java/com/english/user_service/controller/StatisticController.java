package com.english.user_service.controller;

import com.english.dto.response.StatisticResponse;
import com.english.enums.TimeRange;
import com.english.user_service.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistic")
public class StatisticController {

    @Autowired
    StatisticService statisticService;
    @GetMapping("/users")
    public ResponseEntity<StatisticResponse> getUsersStatistic(@RequestParam("time_range")TimeRange timeRange){
        return  ResponseEntity.ok(statisticService.getUsersStatistic(timeRange));
    }
}
