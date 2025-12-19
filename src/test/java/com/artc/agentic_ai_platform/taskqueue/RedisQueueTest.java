package com.artc.agentic_ai_platform.taskqueue;

import com.artc.agentic_ai_platform.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisQueueTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ListOperations<String, Object> listOperations;
    @Mock private ObjectMapper objectMapper;

    private RedisQueue redisQueue;

    @BeforeEach
    void setup() {
        // Mock the intermediate operations call
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        redisQueue = new RedisQueue(redisTemplate, objectMapper);
    }

    @Test
    void push_ShouldDelegateToRedisLeftPush() {
        // Arrange
        Task task = Task.builder().taskId("t1").build();

        // Act
        redisQueue.push(task);

        // Assert
        verify(listOperations).leftPush(eq("task_queue"), eq(task));
    }

    @Test
    void pop_ShouldReturnEmpty_WhenRedisReturnsNull() {
        // Arrange
        when(listOperations.rightPop(eq("task_queue"), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);

        // Act
        Optional<Task> result = redisQueue.pop();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void pop_ShouldDeserializeAndReturnTask_WhenRedisReturnsData() {
        // Arrange
        Object rawRedisObject = new Object(); // Simulating a LinkedHashMap or raw object
        Task expectedTask = Task.builder().taskId("t2").build();

        when(listOperations.rightPop(eq("task_queue"), anyLong(), any(TimeUnit.class)))
                .thenReturn(rawRedisObject);

        // Mock the mapper conversion
        when(objectMapper.convertValue(rawRedisObject, Task.class)).thenReturn(expectedTask);

        // Act
        Optional<Task> result = redisQueue.pop();

        // Assert
        assertTrue(result.isPresent());
        assertEquals("t2", result.get().getTaskId());
    }
}