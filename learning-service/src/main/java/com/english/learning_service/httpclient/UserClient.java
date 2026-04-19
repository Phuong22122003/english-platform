package com.english.learning_service.httpclient;

import com.english.learning_service.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user",url = "${app.services.user}")
public interface UserClient {
    @PostMapping("/users/ids")
    public List<UserResponse> getUserInfos(@RequestBody List<String> ids);
}
