package com.english.api_gateway.configuration;

import com.english.api_gateway.service.IdentityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {
    @NonFinal
    @Value("${app.api-prefix}")
    private String API_PREFIX;
    String[] publicEndPoints = {
            "/user-service/authenticate/signup",
            "/user-service/authenticate/login",
            "/user-service/authenticate/password/otp",
            "/user-service/authenticate/password/otp/validation",
            "/user-service/authenticate/password/resets",
            "/learning-service/plan/agent-generation"
    };

    private IdentityService identityService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<String> authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

        if (isPublicEndPoint(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        if (CollectionUtils.isEmpty(authHeader)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.getFirst().replace("Bearer ", "");
        return identityService.validateToken(token).flatMap(introspectResponseApiResponse -> {
            if (introspectResponseApiResponse.isAuthenticated()) {
                return chain.filter(exchange);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }).onErrorResume(throwable -> {
            log.info(throwable.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        });
    }

    private boolean isPublicEndPoint(ServerHttpRequest request) {
        log.info(request.getURI().getPath());
        return Arrays.stream(publicEndPoints).anyMatch(ep -> request.getURI().getPath().matches(API_PREFIX + ep));
    }

    @Override
    public int getOrder() {
        return -1;
    }
    
}

