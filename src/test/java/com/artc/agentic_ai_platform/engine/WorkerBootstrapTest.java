package com.artc.agentic_ai_platform.engine;

import com.artc.agentic_ai_platform.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerBootstrapTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    @Mock
    private WorkflowEngine workflowEngine;

    @Mock
    private Executor executor;

    @InjectMocks
    private WorkerBootstrap workerBootstrap;

    @Test
    void onApplicationReady_ShouldLaunchConfiguredWorkers() {
        // Arrange
        int configuredWorkers = 5;
        when(appConfig.getQueue().getConcurrency().getWorkers()).thenReturn(configuredWorkers);

        // Act
        workerBootstrap.onApplicationReady();

        // Assert
        // Verify that executor.execute() was called 5 times
        verify(executor, times(configuredWorkers)).execute(any(Runnable.class));

    }
}