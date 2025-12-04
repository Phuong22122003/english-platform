package com.english.content_service.service;

import com.english.content_service.entity.GrammarTopic;
import com.english.content_service.entity.ListeningTopic;
import com.english.content_service.entity.VocabularyTopic;
import org.springframework.stereotype.Service;

@Service
public interface AgentService {
    public void addTopicToVectorDB(VocabularyTopic topic);
    public void addTopicToVectorDB(GrammarTopic grammarTopic);
    public void addTopicToVectorDB(ListeningTopic listeningTopic);
    public void deleteTopicFromVectorDB(String topicId);
}
