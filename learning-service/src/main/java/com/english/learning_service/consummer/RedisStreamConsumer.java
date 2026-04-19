package com.english.learning_service.consummer;

import com.english.learning_service.dto.request.UserRequest;
import com.english.learning_service.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {
    ObjectMapper mapper;
    UserService userService;
    RedisTemplate<String, Object> redisTemplate;
    private final String GROUP = "user_group";
    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> data = message.getValue();

        String userJson = data.get("user");
        UserRequest user = null;
        try {
            user = mapper.readValue(userJson, UserRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        userService.updateUser(user);
        redisTemplate.opsForStream().acknowledge(Objects.requireNonNull(message.getStream()),GROUP, message.getId());
    }
}
