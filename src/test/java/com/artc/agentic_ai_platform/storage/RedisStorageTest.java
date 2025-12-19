package com.artc.agentic_ai_platform.storage;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisStorageTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ObjectMapper objectMapper;

    // Deep stubs for config chain: appConfig.getStorage().getDefaultTtlSec()
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    private RedisStorage redisStorage;

    @BeforeEach
    void setup() {
        // Mock the opsForValue call to return our mock ValueOperations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisStorage = new RedisStorage(redisTemplate, objectMapper, appConfig);
    }

    @Test
    void save_ShouldUseDefaultTtl_WhenNoneProvided() {
        // Arrange
        String key = "test-key";
        String value = "test-value";
        long defaultTtl = 3600L;

        when(appConfig.getStorage().getDefaultTtlSec()).thenReturn(defaultTtl);

        // Act
        redisStorage.save(key, value);

        // Assert
        verify(valueOperations).set(eq(key), eq(value), eq(defaultTtl), eq(TimeUnit.SECONDS));
    }

    @Test
    void save_ShouldUseCustomTtl_WhenProvided() {
        // Arrange
        String key = "test-key";
        String value = "test-value";
        long customTtl = 60L;

        // Act
        redisStorage.save(key, value, customTtl);

        // Assert
        verify(valueOperations).set(eq(key), eq(value), eq(customTtl), eq(TimeUnit.SECONDS));
    }

    @Test
    void get_ShouldReturnEmpty_WhenRedisReturnsNull() {
        // Arrange
        when(valueOperations.get("missing-key")).thenReturn(null);

        // Act
        Optional<String> result = redisStorage.get("missing-key", String.class);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void get_ShouldReturnMappedObject_WhenRedisReturnsData() {
        // Arrange
        String key = "json-key";
        Object redisRawObj = new Object(); // Simulate raw object
        String expectedValue = "mapped-string";

        when(valueOperations.get(key)).thenReturn(redisRawObj);
        when(objectMapper.convertValue(redisRawObj, String.class)).thenReturn(expectedValue);

        // Act
        Optional<String> result = redisStorage.get(key, String.class);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
    }

    @Test
    void delete_ShouldCallTemplateDelete() {
        // Act
        redisStorage.delete("key-to-delete");

        // Assert
        verify(redisTemplate).delete("key-to-delete");
    }
}