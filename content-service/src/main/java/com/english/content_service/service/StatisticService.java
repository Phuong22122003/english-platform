package com.english.content_service.service;

import com.english.content_service.dto.response.TopicViewSummaryResponse;
import com.english.dto.response.StatisticResponse;
import com.english.enums.TimeRange;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StatisticService {
    StatisticResponse getTopicViewTimeSeries(TimeRange timeRange);
    public List<TopicViewSummaryResponse> getTopTopicRanking(int n);
}
