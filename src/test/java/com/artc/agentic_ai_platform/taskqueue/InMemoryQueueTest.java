package com.artc.agentic_ai_platform.taskqueue;

import com.artc.agentic_ai_platform.model.Task;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryQueueTest {

    @Test
    void pushAndPop_ShouldStoreAndRetrieveTask() {
        // Arrange
        InMemoryQueue queue = new InMemoryQueue();
        Task task = Task.builder().taskId("local-1").build();

        // Act
        queue.push(task);
        Optional<Task> result = queue.pop();

        // Assert
        assertTrue(result.isPresent());
        assertEquals("local-1", result.get().getTaskId());
    }

    @Test
    void pop_ShouldReturnEmpty_WhenQueueIsEmpty() {
        // Arrange
        InMemoryQueue queue = new InMemoryQueue();

        // Act
        // This will block for 2 seconds (defined in your class) and then return empty
        long start = System.currentTimeMillis();
        Optional<Task> result = queue.pop();
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertTrue(result.isEmpty());
        // Verify it actually waited (approx 2000ms)
        assertTrue(duration >= 1900, "Should wait for timeout before returning empty");
    }
}