package com.english.content_service.httpclient;

import com.english.content_service.dto.request.TopicRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "Topic",url = "${app.services.agent}")
public interface AgentClient {
    @PostMapping("/topics")
    public void addTopicTopVectorDB(TopicRequest request);
    @DeleteMapping("/topics/{topic_id}")
    public void deleteTopicFromVectorDB(@PathVariable(name = "topic_id") String topicId);
}
