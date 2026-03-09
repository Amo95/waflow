package com.infusi.waflow.session;

import java.util.Optional;

/**
 * Abstraction for session persistence.
 * Implementations can use Redis, in-memory storage, or any other backend.
 */
public interface SessionStore {

    Optional<SessionData> get(String phoneNumber);

    void save(SessionData session);

    void delete(String phoneNumber);
}
