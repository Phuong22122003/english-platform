package com.english.learning_service.service.implt;

import com.english.dto.response.*;
import com.english.exception.NotFoundException;
import com.english.learning_service.dto.request.ExamHistoryRequest;
import com.english.learning_service.dto.request.UserAnswerRequest;
import com.english.learning_service.dto.response.ExamHistoryResponse;
import com.english.learning_service.dto.response.GetGrammarTestQuestionsByTestIdResponse;
import com.english.learning_service.dto.response.GetVocabularyTestQuestionResponse;
import com.english.learning_service.dto.response.QuestionResponse;
import com.english.learning_service.entity.ExamHistory;
import com.english.learning_service.entity.UserAnswer;
import com.english.learning_service.enums.FilterType;
import com.english.learning_service.enums.ItemTypeEnum;
import com.english.learning_service.httpclient.GrammarClient;
import com.english.learning_service.httpclient.ListeningClient;
import com.english.learning_service.httpclient.ToeicClient;
import com.english.learning_service.httpclient.VocabularyClient;
import com.english.learning_service.mapper.ExamHistoryMapper;
import com.english.learning_service.repository.ExamHistoryRepository;
import com.english.learning_service.repository.UserAnswerRepository;
import com.english.learning_service.service.ExamHistoryService;
import com.english.utilities.Common;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@Data
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamHistoryServiceImplt implements ExamHistoryService {
    private ExamHistoryMapper examHistoryMapper;
    private ExamHistoryRepository examHistoryRepository;
    private UserAnswerRepository userAnswerRepository;
    private VocabularyClient vocabularyClient;
    private GrammarClient grammarClient;
    private ListeningClient listeningClient;
    private ToeicClient toeicClient;
    @Override
    @Transactional
    public ExamHistoryResponse addExamHistory(ExamHistoryRequest request) {
        if(request.getTestType().equals(ItemTypeEnum.FULL_TEST)){
            toeicClient.updateTotalCompletion(request.getTestId());
        }
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();
        ExamHistory examHistory = examHistoryMapper.toExamHistory(request);

        examHistory.setUserId(userId);

        Map<String,QuestionResponse> questionMap = new HashMap<>();
        switch (request.getTestType()){
            case ItemTypeEnum.VOCABULARY -> {
                GetVocabularyTestQuestionResponse vocab = vocabularyClient.getTestQuestionsByTestId(examHistory.getTestId());
                examHistory.setName(vocab.getTestName());
                examHistory.setDuration(vocab.getDuration());
                for(var q: vocab.getQuestions()){
                    questionMap.put(q.getId(),QuestionResponse.builder()
                            .options(q.getOptions())
                            .correctAnswer(q.getCorrectAnswer())
                            .question(q.getQuestion())
                            .imageUrl(q.getImageUrl())
                            .explanation(q.getExplaination())
                            .build());
                }
                break;
            }
            case ItemTypeEnum.GRAMMAR -> {
                GetGrammarTestQuestionsByTestIdResponse grammar = grammarClient.getTestQuestionsByTestId(examHistory.getTestId());
                examHistory.setName(grammar.getTestName());
                examHistory.setDuration(grammar.getDuration());
                for(var q: grammar.getGrammarTestQuestions()){
                    questionMap.put(q.getId(),QuestionResponse.builder()
                            .options(q.getOptions())
                            .correctAnswer(q.getCorrectAnswer())
                            .question(q.getQuestion())
                            .explanation(q.getExplaination())
                            .build());
                }
                break;
            }
            case ItemTypeEnum.LISTENING -> {
                com.english.learning_service.dto.response.ListeningTestReponse listening = listeningClient.getTestDetail(examHistory.getTestId());
                examHistory.setName(listening.getName());
                examHistory.setDuration(listening.getDuration());
                for(var q: listening.getQuestions()){
                    questionMap.put(q.getId(),QuestionResponse.builder()
                            .options(q.getOptions())
                            .correctAnswer(q.getCorrectAnswer())
                            .question(q.getQuestion())
                            .audioUrl(q.getAudioUrl())
                            .imageUrl(q.getImageUrl())
                            .explanation(q.getExplaination())
                            .build());
                }
                break;
            }
            case ItemTypeEnum.FULL_TEST -> {
                ToeicTestResponse toeic = toeicClient.getTestDetail(examHistory.getTestId());
                examHistory.setName(toeic.getName());
                examHistory.setDuration(200);
                for(var q: toeic.getQuestions()){
                    questionMap.put(q.getId(),QuestionResponse.builder()
                            .options(q.getOptions())
                            .correctAnswer(q.getCorrectAnswer())
                            .question(q.getQuestion())
                            .audioUrl(q.getAudioUrl())
                            .imageUrl(q.getImageUrl())
                            .explanation(q.getExplanation())
                            .part(q.getPart())
                            .build());
                }
            }
        }

        ExamHistory savedEntity = examHistoryRepository.save(examHistory);
        List<UserAnswer> userAnswers = new LinkedList<>();
        for (UserAnswerRequest ua: request.getAnswers()) {
            UserAnswer userAnswer = UserAnswer
                    .builder()
                    .selectedAnswer(ua.getSelectedAnswer())
                    .examHistory(savedEntity)
                    .isCorrect(ua.isCorrect())
                    .questionId(ua.getQuestionId())
                    .part(ua.getPart())
                    .build();
            QuestionResponse q = questionMap.get(ua.getQuestionId());
            if(q!=null){
                userAnswer.setQuestion(q.getQuestion());
                userAnswer.setOptions(q.getOptions());
                userAnswer.setCorrectAnswer(q.getCorrectAnswer());
                userAnswer.setExplanation(q.getExplanation());
                userAnswer.setAudioUrl(q.getAudioUrl());
                userAnswer.setImageUrl(q.getImageUrl());
            }
            userAnswers.add(userAnswer);
        }
        userAnswerRepository.saveAll(userAnswers);
        return examHistoryMapper.toExamHistoryResponse(examHistory);
    }

    @Override
    @Transactional
    public Page<ExamHistoryResponse> getExamHistories(int page, int limit, FilterType filterType) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();
        var pageable = org.springframework.data.domain.PageRequest.of(page, limit);
        Page<ExamHistory> histories;
        if(filterType.equals(FilterType.ALL)){
            histories = examHistoryRepository.findByUserId(userId, pageable);
        }
        else{
            histories = examHistoryRepository.findByUserIdAndTestType(userId,ItemTypeEnum.map(filterType),pageable);
        }
        List<ExamHistoryResponse> examHistoryResponses = examHistoryMapper.toExamHistoryResponses(histories.getContent());
        for(var h: examHistoryResponses){
            if (h.getTestType().equals(ItemTypeEnum.FULL_TEST)) {
                h.setDuration(200);
            }
        }
        return new PageImpl<>(examHistoryResponses,histories.getPageable(),histories.getTotalElements());
    }


    @Override
    @Transactional
    public ExamHistoryResponse getExamHistoryDetail(String examHistoryId) {
        ExamHistory examHistory = examHistoryRepository.findById(examHistoryId).orElseThrow(()-> new NotFoundException("Exam history not found"));
        ExamHistoryResponse examHistoryResponse = examHistoryMapper.toExamHistoryResponse(examHistory);
        List<UserAnswer> userAnswers = userAnswerRepository.findByExamHistoryId(examHistoryId);

        List<QuestionResponse> questions = new ArrayList<>();
        userAnswers.forEach(a->{
            questions.add(QuestionResponse.builder()
                    .options(a.getOptions())
                    .correctAnswer(a.getCorrectAnswer())
                    .userAnswer(a.getSelectedAnswer())
                    .question(a.getQuestion())
                    .explanation(a.getExplanation())
                    .build());
        });
        examHistoryResponse.setQuestions(questions);
        return examHistoryResponse;
    }
}
