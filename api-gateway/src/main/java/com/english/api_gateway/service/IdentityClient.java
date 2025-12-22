package com.english.api_gateway.service;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import com.english.api_gateway.dto.IntrospectRequest;
import com.english.api_gateway.dto.IntrospectResponse;

import reactor.core.publisher.Mono;

public interface IdentityClient {
    @PostExchange(url = "/user-service/authenticate/introspect", contentType = MediaType.APPLICATION_JSON_VALUE)
    Mono<IntrospectResponse> introspect(@RequestBody IntrospectRequest token);
}