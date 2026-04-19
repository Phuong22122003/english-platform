package com.english.learning_service.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.data.redis.stream.Subscription;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisStreamConfiguration {
    private final String STREAM_KEY = "user_stream";
    private final String GROUP = "user_group";

    @Bean
    public Subscription subscription(RedisConnectionFactory factory, StreamListener<String, MapRecord<String, String, String>> listener) {
        initStreamAndGroup(factory);

        var options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(60))
                .build();

        var container = StreamMessageListenerContainer.create(factory, options);

        var subscription = container.receive(
                Consumer.from(GROUP, "consumer-1"),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                listener
        );
        container.receive(
                Consumer.from(GROUP, "consumer-1"),
                StreamOffset.create(STREAM_KEY, ReadOffset.from("0")),
                listener
        );
        container.start();
        return subscription;
    }

    private void initStreamAndGroup(RedisConnectionFactory factory) {
        try {

            factory.getConnection().streamCommands().xGroupCreate(
                    STREAM_KEY.getBytes(),
                    GROUP,
                    ReadOffset.latest(),
                    true
            );
        } catch (RedisSystemException e) {
            if (e.getRootCause() != null && e.getRootCause().getMessage().contains("BUSYGROUP")) {
                System.out.println("Consumer Group existed");
            } else {
                throw e;
            }
        }
    }
}
