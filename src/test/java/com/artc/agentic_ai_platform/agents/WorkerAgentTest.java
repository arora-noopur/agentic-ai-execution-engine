package com.artc.agentic_ai_platform.agents;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.constants.AppConstants;
import com.artc.agentic_ai_platform.core.IAgentTool;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.llm.MockLLMService;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerAgentTest {

    @Mock private IAgentTool mockTool;
    @Mock private IStorageBackend storage;
    @Mock private MockLLMService llmService;
    @Mock private Executor executor;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private AppConfig appConfig;

    private WorkerAgent workerAgent;

    @BeforeEach
    void setup() {
        when(mockTool.getName()).thenReturn("TEST_TOOL");

        // FORCE SYNCHRONOUS EXECUTION
        // When executor.execute(runnable) is called, run it immediately
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        workerAgent = new WorkerAgent(List.of(mockTool), storage, llmService, executor, appConfig);
    }

    @Test
    void process_ShouldExecuteTool_AndTriggerReviewer() {
        // --- ARRANGE ---
        Task task = Task.builder()
                .workflowId("wf-202")
                .toolName("TEST_TOOL")
                .toolArguments(List.of("input-A"))
                .userRequest("Analyze this")
                .build();

        when(appConfig.getAgents().getWorkers().isEnabled()).thenReturn(true);
        when(mockTool.execute("input-A|error")).thenReturn("Raw Tool Output"); // "error" is default keyword
        when(llmService.generate(anyString(), anyString())).thenReturn("AI Interpretation");

        // --- ACT ---
        List<Task> result = workerAgent.process(task);

        // --- ASSERTIONS ---
        // 1. Verify Tool called
        verify(mockTool).execute(contains("input-A"));

        // 2. Verify Storage (Result saved with TTL)
        verify(storage).save(
                eq(String.format(AppConstants.KEY_TOOL_RESULT,"wf-202","TEST_TOOL")),
                contains("AI Interpretation")
        );



        // 3. Verify Next Step is Reviewer
        assertEquals(1, result.size());
        assertEquals(AgentType.REVIEWER, result.get(0).getTargetAgent());
    }
}