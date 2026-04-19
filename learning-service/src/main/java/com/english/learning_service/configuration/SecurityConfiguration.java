package com.english.learning_service.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Autowired
    private CustomJwtDecoder customJwtDecoder;
    final String[] PUBLIC_URLS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/authenticate/**",
            "/plan/agent-generation",
            "/plan/callback",
            "/favorites/ids",
            "/comments/tests/*/sse"
    };
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests((request)->
            request.requestMatchers(PUBLIC_URLS).permitAll()
                    .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwtConfigurer -> jwtConfigurer.decoder(customJwtDecoder)
                            .jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
        .csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return converter;
    }

}
