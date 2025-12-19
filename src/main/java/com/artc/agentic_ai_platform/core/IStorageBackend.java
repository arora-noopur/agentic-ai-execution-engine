package com.artc.agentic_ai_platform.core;

import java.util.Optional;

public interface IStorageBackend {
    void save(String key, Object value);
    void save(String key, Object value, long ttlSeconds);
    <T> Optional<T> get(String key, Class<T> clazz);
    void delete(String key);
}