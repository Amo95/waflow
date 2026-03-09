package com.infusi.waflow.session;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory session store for development and testing.
 * Sessions are lost on restart.
 */
@Slf4j
public class InMemorySessionStore implements SessionStore {

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public InMemorySessionStore() {
        log.info("Using in-memory session store (sessions will not survive restarts)");
    }

    @Override
    public Optional<SessionData> get(String phoneNumber) {
        return Optional.ofNullable(sessions.get(phoneNumber));
    }

    @Override
    public void save(SessionData session) {
        sessions.put(session.getPhoneNumber(), session);
    }

    @Override
    public void delete(String phoneNumber) {
        sessions.remove(phoneNumber);
        log.debug("Cleared session for {}", phoneNumber);
    }
}
