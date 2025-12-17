package com.english.user_service.service.implt;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import com.english.dto.request.MailRequest;
import com.english.exception.UnauthorizedException;
import com.english.service.EmailService;
import com.english.user_service.dto.request.*;
import com.english.user_service.dto.response.VerifyOtpResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.english.user_service.dto.response.IntrospectResponse;
import com.english.user_service.dto.response.UserLoginResponse;
import com.english.user_service.entity.User;
import com.english.user_service.repository.UserRepository;
import com.english.user_service.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImplt implements AuthenticationService {
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected Long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected Long VALID_DURATION;

    UserRepository userRepository;
    RedisTemplate<String,Object> redisTemplate;
    PasswordEncoder passwordEncoder;
    EmailService emailService;
    @Override
    public IntrospectResponse validateToken(IntrospectRequest request) {
        String token = request.getToken();
        Boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (Exception e) {
            isValid = false;
        }
        return IntrospectResponse.builder().isAuthenticated(isValid).build();
    }


    public String generateToken(User user) {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .issuer("EnglishWebsite")
                .issueTime(new Date())
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .claim("scope", "ROLE_"+user.getRole().toString())
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = isRefresh
                ? new Date((signedJWT.getJWTClaimsSet().getIssueTime().toInstant().plus(REFRESHABLE_DURATION,
                        ChronoUnit.SECONDS)).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean verified = signedJWT.verify(verifier);
        boolean result = verified && expiryTime.after(new Date());
        if (!result) {
            throw new UnauthorizedException("Unauthorize");
        }
        return signedJWT;
    }

    @Override
    public UserLoginResponse login(UserLogInRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String username = request.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        String password = request.getPassword();
        boolean authenticated = passwordEncoder.matches(password, user.getPassword());
        if (!authenticated) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = generateToken(user);

        return new UserLoginResponse(token);
    }
    public void sendOtp(ForgotPasswordRequest request){
        if(!userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email not found");
        }
        Random random = new Random();
        int number = random.nextInt(10000); // 0 -> 9999
        String otp = String.format("%04d", number);
        // send email
        emailService.sendEmail(
                MailRequest
                        .builder()
                        .to(request.getEmail())
                        .subject("Password Reset OTP")
                        .body("Your OTP is: " + otp)
                        .build());
        redisTemplate.opsForValue().set(request.getEmail(),otp);
    }

    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        Object otp = redisTemplate.opsForValue().get(request.getEmail());
        if(otp==null){
            throw new RuntimeException("Time out");
        }
        if(!otp.toString().equals(request.getOtp()))
            throw new RuntimeException("Invalid otp");
        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(resetToken,request.getEmail());
        return new VerifyOtpResponse(resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Object email = redisTemplate.opsForValue().get(request.getResetToken());
        if(email==null) throw new RuntimeException("Time out");
        User user = userRepository.findByEmail(email.toString()).orElseThrow(()->{
            return  new RuntimeException("Email not found");
        });
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
