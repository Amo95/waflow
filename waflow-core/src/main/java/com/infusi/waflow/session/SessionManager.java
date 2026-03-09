package com.infusi.waflow.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class SessionManager {

    private final SessionStore store;

    public SessionManager(SessionStore store) {
        this.store = store;
    }

    public SessionData getOrCreate(String phoneNumber) {
        return get(phoneNumber).orElseGet(() -> createSession(phoneNumber));
    }

    public Optional<SessionData> get(String phoneNumber) {
        return store.get(phoneNumber);
    }

    public void save(SessionData session) {
        store.save(session);
    }

    public void updateStep(String phoneNumber, String stepId) {
        SessionData session = getOrCreate(phoneNumber);
        session.setCurrentStep(stepId);
        store.save(session);
    }

    public void updateData(String phoneNumber, String key, Object value) {
        SessionData session = getOrCreate(phoneNumber);
        session.getData().put(key, value);
        store.save(session);
    }

    public void switchFlow(String phoneNumber, String flowId, String step) {
        SessionData session = getOrCreate(phoneNumber);
        session.setCurrentFlow(flowId);
        if (step != null) {
            session.setCurrentStep(step);
        }
        store.save(session);
    }

    public void clear(String phoneNumber) {
        store.delete(phoneNumber);
    }

    private SessionData createSession(String phoneNumber) {
        SessionData session = SessionData.builder()
                .phoneNumber(phoneNumber)
                .build();
        store.save(session);
        return session;
    }
}
