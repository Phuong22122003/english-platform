package com.english.learning_service.service;

import com.english.learning_service.dto.request.CallBackRequest;
import com.english.learning_service.dto.request.PlanIntentRequest;
import com.english.learning_service.dto.request.PlanRequest;
import com.english.learning_service.dto.response.PlanResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public interface PlanService {
    PlanResponse addPlan(PlanRequest request);
    @Deprecated(since = "Planning by agent is removed")
    SseEmitter addPlanByAgent(PlanIntentRequest request);
    @Deprecated(since = "Planning by agent is removed")
    void sendNotification(CallBackRequest planRequest);
    Page<PlanResponse> getPlan(int page, int size);
    PlanResponse getPlanDetail(String planId);
    void deletePlan(String planId);
    PlanResponse editPlan(String planId, PlanRequest planRequest);
}
