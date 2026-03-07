package com.infusi.waflow.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class SessionManager {

    private static final String KEY_PREFIX = "waflow:session:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public SessionManager(RedisTemplate<String, String> redisTemplate,
                          ObjectMapper objectMapper,
                          @Value("${waflow.session.timeout:PT24H}") Duration timeout) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.timeout = timeout;
    }

    public SessionData getOrCreate(String phoneNumber) {
        return get(phoneNumber).orElseGet(() -> createSession(phoneNumber));
    }

    public Optional<SessionData> get(String phoneNumber) {
        String key = KEY_PREFIX + phoneNumber;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, SessionData.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session for {}", phoneNumber, e);
            return Optional.empty();
        }
    }

    public void save(SessionData session) {
        String key = KEY_PREFIX + session.getPhoneNumber();
        session.setUpdatedAt(Instant.now());
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, timeout);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session for {}", session.getPhoneNumber(), e);
            throw new RuntimeException("Failed to save session", e);
        }
    }

    public void updateStep(String phoneNumber, String stepId) {
        SessionData session = getOrCreate(phoneNumber);
        session.setCurrentStep(stepId);
        save(session);
    }

    public void updateData(String phoneNumber, String key, Object value) {
        SessionData session = getOrCreate(phoneNumber);
        session.getData().put(key, value);
        save(session);
    }

    public void switchFlow(String phoneNumber, String flowId, String step) {
        SessionData session = getOrCreate(phoneNumber);
        session.setCurrentFlow(flowId);
        if (step != null) {
            session.setCurrentStep(step);
        }
        save(session);
    }

    public void clear(String phoneNumber) {
        String key = KEY_PREFIX + phoneNumber;
        redisTemplate.delete(key);
        log.debug("Cleared session for {}", phoneNumber);
    }

    private SessionData createSession(String phoneNumber) {
        SessionData session = SessionData.builder()
                .phoneNumber(phoneNumber)
                .build();
        save(session);
        return session;
    }
}
