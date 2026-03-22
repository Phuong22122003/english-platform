package com.english.api_gateway.configuration;

import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class GlobalErrorConfiguration {
    @Bean
    @Order(-2)
    public ErrorWebExceptionHandler globalErrorWebExceptionHandler(
            ServerCodecConfigurer serverCodecConfigurer) {

        return (exchange, ex) -> {
            Map<String, Object> errorAttributes = new HashMap<>();
            errorAttributes.put("timestamp", Instant.now().toString());
            errorAttributes.put("path", exchange.getRequest().getPath().value());

            HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
            String message = "Server error";

            if (ex instanceof ResponseStatusException rse) {
                status = rse.getStatusCode();
                message = rse.getReason();
            } else if (ex instanceof IllegalArgumentException) {
                status = HttpStatus.BAD_REQUEST;
                message = "Bad request";
            } else if (ex instanceof WebClientRequestException ||
                    ex.getCause() instanceof ConnectException ||
                    ex instanceof ReadTimeoutException) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "Service Unavailable";
            }

            errorAttributes.put("status", status.value());
            errorAttributes.put("message", message);
            errorAttributes.put("error",((HttpStatus)status).getReasonPhrase());

            return ServerResponse
                    .status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(errorAttributes).flatMap(serverResponse -> serverResponse.writeTo(exchange, new ServerResponse.Context() {
                        @Override
                        public List<HttpMessageWriter<?>> messageWriters() {
                            return serverCodecConfigurer.getWriters();
                        }

                        @Override
                        public List<ViewResolver> viewResolvers() {
                            return List.of();
                        }
                    }));
        };
    }
}
