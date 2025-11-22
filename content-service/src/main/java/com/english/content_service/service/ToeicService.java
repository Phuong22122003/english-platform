package com.english.content_service.service;

import com.english.content_service.dto.request.ToeicTestGroupRequest;
import com.english.content_service.dto.request.ToeicTestRequest;
import com.english.dto.response.ToeicTestGroupResponse;
import com.english.dto.response.ToeicTestResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface ToeicService {
    //group
    public Page<ToeicTestGroupResponse> getGroups(int page, int limit);
    public ToeicTestGroupResponse addTestGroup(ToeicTestGroupRequest request);
    public void deleteTestGroup(String id);
    public ToeicTestGroupResponse updateTestGroup(String id,ToeicTestGroupRequest request);
    //test
    public ToeicTestResponse getTestById(String id);
    public ToeicTestResponse addTest(String groupId,ToeicTestRequest request, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);
    public void deleteTest(String id);
    public ToeicTestResponse updateTest(String id,ToeicTestRequest request, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);

}
