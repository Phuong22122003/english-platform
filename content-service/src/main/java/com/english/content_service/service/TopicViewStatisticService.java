package com.english.content_service.service;

import com.english.enums.TopicType;
import org.springframework.stereotype.Service;


@Service
public interface TopicViewStatisticService {
    public void addTopic(String topicId, TopicType topicType);
}
