package com.infusi.waflow.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Redis-backed session store for production use.
 */
@Slf4j
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "waflow:session:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public RedisSessionStore(RedisTemplate<String, String> redisTemplate,
                             ObjectMapper objectMapper,
                             Duration timeout) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        log.info("Using Redis session store (timeout={})", timeout);
    }

    @Override
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

    @Override
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

    @Override
    public void delete(String phoneNumber) {
        String key = KEY_PREFIX + phoneNumber;
        redisTemplate.delete(key);
        log.debug("Cleared session for {}", phoneNumber);
    }
}
