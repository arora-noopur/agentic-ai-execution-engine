package com.artc.agentic_ai_platform.engine;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.constants.AppConstants;
import com.artc.agentic_ai_platform.core.IAgent;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.ITaskQueue;
import com.artc.agentic_ai_platform.core.exception.RetryableException;
import com.artc.agentic_ai_platform.core.exception.TerminalException;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    @Mock private ITaskQueue queue;
    @Mock private IStorageBackend storage;
    @Mock private IAgent mockAgent;

    private WorkflowEngine workflowEngine;

    @BeforeEach
    void setup() {
        // Setup Agent Map
        when(mockAgent.getType()).thenReturn(AgentType.WORKER);
        List<IAgent> agents = List.of(mockAgent);

        workflowEngine = new WorkflowEngine(appConfig, queue, agents, storage);
    }

    /**
     * Helper to break the infinite loop after 1 iteration.
     * 1. Returns the 'task' first.
     * 2. On second call, interrupts thread and returns empty to trigger the break condition.
     */
    private void mockQueueOneShot(Task task) {
        when(queue.pop()).thenReturn(Optional.of(task))
                .thenAnswer((Answer<Optional<Task>>) invocation -> {
                    Thread.currentThread().interrupt(); // Signal stop
                    return Optional.empty();
                });
    }

    @Test
    void runConsumerLoop_ShouldProcessTask_AndPushDownstream() {
        // Arrange
        Task inputTask = Task.builder()
                .workflowId("wf-1")
                .taskId("t-1")
                .targetAgent(AgentType.WORKER)
                .build();

        Task downstreamTask = Task.builder().taskId("t-2").build();

        mockQueueOneShot(inputTask);
        when(mockAgent.process(inputTask)).thenReturn(List.of(downstreamTask));

        // Act
        workflowEngine.runConsumerLoop(1);

        // Assert
        verify(mockAgent).process(inputTask);
        verify(queue).push(downstreamTask); // Verify output was queued
    }

    @Test
    void runConsumerLoop_ShouldHandleRetryableException() {
        // Arrange
        Task task = Task.builder().workflowId("wf-retry").targetAgent(AgentType.WORKER).retryCount(0).build();

        mockQueueOneShot(task);
        when(mockAgent.process(task)).thenThrow(new RetryableException("API Timeout"));

        // Mock Config for Backoff
        when(appConfig.getQueue().getConcurrency().getMaxRetries()).thenReturn(3);
        when(appConfig.getQueue().getConcurrency().getBackoffStrategy()).thenReturn("fixed");

        // Act
        workflowEngine.runConsumerLoop(1);

        // Assert
        verify(queue).push(task); // Should push back to queue
        assert(task.getRetryCount() == 1); // Count incremented
    }

    @Test
    void runConsumerLoop_ShouldFail_WhenMaxRetriesReached() {
        // Arrange
        Task task = Task.builder().workflowId("wf-max").targetAgent(AgentType.WORKER).retryCount(3).build();

        mockQueueOneShot(task);
        when(appConfig.getQueue().getConcurrency().getMaxRetries()).thenReturn(3);

        // Agent throws retryable, but we are at max limit
        when(mockAgent.process(task)).thenThrow(new RetryableException("Again?"));

        // Act
        workflowEngine.runConsumerLoop(1);

        // Assert
        verify(queue, never()).push(task); // Should DROP, not push
        verify(storage).save(eq(String.format(AppConstants.KEY_STATUS,"wf-max")), eq(WorkflowStatus.FAILED.name())); // Should mark Failed
    }

    @Test
    void runConsumerLoop_ShouldHandleTerminalException() {
        // Arrange
        Task task = Task.builder().workflowId("wf-term").targetAgent(AgentType.WORKER).build();

        mockQueueOneShot(task);
        when(mockAgent.process(task)).thenThrow(new TerminalException("Bad Data"));

        // Act
        workflowEngine.runConsumerLoop(1);

        // Assert
        verify(queue, never()).push(task); // Drop immediately
        verify(storage).save(eq(String.format(AppConstants.KEY_STATUS,"wf-term")), eq(WorkflowStatus.FAILED.name()));
        verify(storage).save(contains("error"), contains("Bad Data"));
    }

    @Test
    void runConsumerLoop_ShouldHandleUnexpectedCrash() {
        // Arrange
        Task task = Task.builder().workflowId("wf-crash").targetAgent(AgentType.WORKER).build();

        mockQueueOneShot(task);
        when(mockAgent.process(task)).thenThrow(new RuntimeException("Null Pointer Oops"));

        // Act
        workflowEngine.runConsumerLoop(1);

        // Assert
        verify(storage).save(eq(String.format(AppConstants.KEY_STATUS,"wf-crash")), eq(WorkflowStatus.FAILED.name()));
        verify(storage).save(contains("error"), contains("Null Pointer Oops"));
    }
}