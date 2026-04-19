package com.english.api_gateway.configuration;

import com.english.api_gateway.service.IdentityService;
import io.netty.handler.timeout.ReadTimeoutException;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
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
            "/learning-service/plan/agent-generation",
            "/learning-service/favorites/ids",
            "/content-service/**",
            "/learning-service/comments/tests/**/sse"
    };
    private AntPathMatcher pathMatcher = new AntPathMatcher();
    private IdentityService identityService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<String> authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

        if (isPublicEndPoint(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        if (CollectionUtils.isEmpty(authHeader)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access"));
        }

        String token = authHeader.getFirst().replace("Bearer ", "");
        return identityService.validateToken(token).flatMap(introspectResponseApiResponse -> {
            if (introspectResponseApiResponse.isAuthenticated()) {
                return chain.filter(exchange);
            } else {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access"));
            }
        }).onErrorResume(Mono::error);
    }

    private boolean isPublicEndPoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        if(path.contains("/comments/tests/") && path.contains("/sse") && method == HttpMethod.GET) return true;

        return Arrays.stream(publicEndPoints).anyMatch(ep -> {
            String fullPattern = API_PREFIX + ep;

            // 1. Nếu là các endpoint login/signup thì cho qua mọi Method
            if (ep.contains("/authenticate/")) {
                return pathMatcher.match(fullPattern, path);
            }

            // 2. Nếu là vocabulary/grammar/listening, CHỈ cho qua nếu là GET
            if (ep.contains("/content-service/")) {
                return method == HttpMethod.GET && pathMatcher.match(fullPattern, path);
            }


            // 3. Mặc định các cái khác trong list public
            return pathMatcher.match(fullPattern, path);
        });
    }

    @Override
    public int getOrder() {
        return -1;
    }
    
}

