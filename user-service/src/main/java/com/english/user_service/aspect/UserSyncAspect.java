package com.english.user_service.aspect;

import com.english.user_service.annotation.SyncUserStream;
import com.english.user_service.dto.request.UserStreamRequest;
import com.english.user_service.dto.response.UserResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor // only assign bean for final filed
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSyncAspect {
    private RedisTemplate<String, Object> redisTemplate;
    private ObjectMapper mapper;
    @AfterReturning(pointcut = "@annotation(syncUserStream)", returning = "result")
    public void afterUserAction(SyncUserStream syncUserStream, Object result) throws JsonProcessingException {

        UserResponse userResponse = (UserResponse) result;
        UserStreamRequest request = UserStreamRequest.builder()
                .id(userResponse.getId())
                .requestTime(LocalDateTime.now())
                .avatarUrl(userResponse.getAvatarUrl())
                .username(userResponse.getUsername())
                .action(syncUserStream.action())
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("user",request);

        redisTemplate.opsForStream().add("user_stream",data);
    }
}
