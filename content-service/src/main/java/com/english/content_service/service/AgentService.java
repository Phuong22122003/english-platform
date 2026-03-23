package com.english.content_service.service;

import com.english.content_service.entity.GrammarTopic;
import com.english.content_service.entity.ListeningTopic;
import com.english.content_service.entity.VocabularyTopic;
import org.springframework.stereotype.Service;

@Service
@Deprecated(since = "Planning by agent is removed",forRemoval = true)
public interface AgentService {
    @Deprecated
    public void addTopicToVectorDB(VocabularyTopic topic);
    @Deprecated
    public void addTopicToVectorDB(GrammarTopic grammarTopic);
    @Deprecated
    public void addTopicToVectorDB(ListeningTopic listeningTopic);
    @Deprecated
    public void deleteTopicFromVectorDB(String topicId);
}
