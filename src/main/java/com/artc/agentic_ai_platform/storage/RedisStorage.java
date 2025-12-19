package com.artc.agentic_ai_platform.storage;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisStorage implements IStorageBackend {

    private final RedisTemplate<String, Object> template;
    private final ObjectMapper mapper;
    private final AppConfig appConfig;

    @Override
    public void save(String k, Object v) {
        template.opsForValue().set(k, v, appConfig.getStorage().getDefaultTtlSec(), TimeUnit.SECONDS);
    }

    public void save(String k, Object v, long ttl) {
        template.opsForValue().set(k, v, ttl, TimeUnit.SECONDS);
    }

    public <T> Optional<T> get(String k, Class<T> c) {
        Object o = template.opsForValue().get(k);
        return o == null ? Optional.empty() : Optional.of(mapper.convertValue(o, c));
    }

    public void delete(String k) {
        template.delete(k);
    }
}
