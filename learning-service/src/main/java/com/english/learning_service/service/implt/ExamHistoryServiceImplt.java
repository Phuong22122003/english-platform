package com.english.learning_service.service.implt;

import com.english.dto.response.*;
import com.english.exception.NotFoundException;
import com.english.learning_service.dto.request.ExamHistoryRequest;
import com.english.learning_service.dto.response.ExamHistoryResponse;
import com.english.learning_service.entity.ExamHistory;
import com.english.learning_service.entity.UserAnswerGroup;
import com.english.learning_service.enums.FilterType;
import com.english.learning_service.enums.ItemTypeEnum;
import com.english.learning_service.mapper.ExamHistoryMapper;
import com.english.learning_service.repository.ExamHistoryRepository;
import com.english.learning_service.repository.UserAnswerGroupRepository;
import com.english.learning_service.repository.UserAnswerRepository;
import com.english.learning_service.service.ExamHistoryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private UserAnswerGroupRepository userAnswerGroupRepository;

    @Override
    @Transactional
    public ExamHistoryResponse addExamHistory(ExamHistoryRequest request) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();
        ExamHistory examHistory = examHistoryMapper.toExamHistory(request);
        examHistory.setUserId(userId);
        examHistoryRepository.save(examHistory);

        List<UserAnswerGroup> answerGroups = examHistoryMapper.toAnswerGroups(request.getAnswerGroups());
        answerGroups.forEach(
                gr ->{
                    gr.getAnswers().forEach(a -> a.setAnswerGroup(gr));
                }
        );
        userAnswerGroupRepository.saveAll(answerGroups);
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
        List<UserAnswerGroup> userAnswerGroups = userAnswerGroupRepository.findByExamHistoryId(examHistoryId);
        examHistoryResponse.setAnswerGroups(examHistoryMapper.toUserAnswerGroupResponses(userAnswerGroups));
        return examHistoryResponse;
    }
}
