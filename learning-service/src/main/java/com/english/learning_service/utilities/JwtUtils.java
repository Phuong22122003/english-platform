package com.english.learning_service.utilities;

import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

public class JwtUtils {
    public static String extractUserId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Object userId = signedJWT.getJWTClaimsSet().getClaim("sub");
            return userId != null ? userId.toString() : null;
        } catch (ParseException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
}
