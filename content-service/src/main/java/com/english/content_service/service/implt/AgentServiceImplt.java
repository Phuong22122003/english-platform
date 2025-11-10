package com.english.content_service.service.implt;

import com.english.content_service.dto.request.TopicRequest;
import com.english.content_service.entity.GrammarTopic;
import com.english.content_service.entity.ListeningTopic;
import com.english.content_service.entity.VocabularyTopic;
import com.english.content_service.httpclient.AgentClient;
import com.english.content_service.service.AgentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AgentServiceImplt implements AgentService {
    AgentClient agentClient;
    @Override
    public void addTopicToVectorDB(VocabularyTopic topic) {
        TopicRequest topicRequest = new TopicRequest();
        topicRequest.setId(topic.getId());
        topicRequest.setName(topic.getName());
        topicRequest.setDescription(topic.getDescription());
        topicRequest.setTopic_type("VOCABULARY");
        topicRequest.setLevel(topic.getLevel());
        agentClient.addTopicTopVectorDB(topicRequest);
    }

    @Override
    public void addTopicToVectorDB(GrammarTopic grammarTopic) {
        TopicRequest topicRequest = new TopicRequest();
        topicRequest.setId(grammarTopic.getId());
        topicRequest.setName(grammarTopic.getName());
        topicRequest.setDescription(grammarTopic.getDescription());
        topicRequest.setTopic_type("GRAMMAR");
        topicRequest.setLevel(grammarTopic.getLevel());
        agentClient.addTopicTopVectorDB(topicRequest);
    }

    @Override
    public void addTopicToVectorDB(ListeningTopic listeningTopic) {
        TopicRequest topicRequest = new TopicRequest();
        topicRequest.setId(listeningTopic.getId());
        topicRequest.setName(listeningTopic.getName());
        topicRequest.setDescription(listeningTopic.getDescription());
        topicRequest.setTopic_type("LISTENING");
        topicRequest.setLevel(listeningTopic.getLevel());
        agentClient.addTopicTopVectorDB(topicRequest);
    }
}
