package com.english.learning_service.service;

import com.english.dto.response.StatisticResponse;
import com.english.enums.TimeRange;
import com.english.enums.TopicType;
import com.english.learning_service.dto.response.UserScore;
import com.english.learning_service.enums.ItemTypeEnum;
import org.springframework.stereotype.Service;

@Service
public interface StatisticService {
    StatisticResponse getNumberOfTestIsTaken(TimeRange timeRange);
    UserScore getUserScores(TimeRange timeRange, ItemTypeEnum filterType);
}
