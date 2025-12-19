package com.artc.agentic_ai_platform.agents;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.llm.MockLLMService;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewerAgentTest {

    @Mock private IStorageBackend storage;
    @Mock private MockLLMService llmService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private AppConfig appConfig;

    private ReviewerAgent reviewerAgent;

    @BeforeEach
    void setup() {
        reviewerAgent = new ReviewerAgent(storage, llmService, appConfig);
    }

    @Test
    void process_ShouldWait_WhenResultsMissing() {
        // --- ARRANGE ---
        String wfId = "wf-wait";
        Task task = Task.builder().workflowId(wfId).build();

        when(appConfig.getAgents().getReviewer().isEnabled()).thenReturn(true);
        // Status is valid (not done)
        when(storage.get(eq("wf:" + wfId + ":status"), eq(String.class))).thenReturn(Optional.of("IN_PROGRESS"));

        // Manifest expects 2 tools
        when(storage.get(eq("wf:" + wfId + ":manifest"), eq(String.class)))
                .thenReturn(Optional.of("TOOL_A,TOOL_B"));

        // Only Tool A is done
        when(storage.get(eq("wf:" + wfId + ":res:TOOL_A"), eq(String.class))).thenReturn(Optional.of("Res A"));
        when(storage.get(eq("wf:" + wfId + ":res:TOOL_B"), eq(String.class))).thenReturn(Optional.empty()); // Missing!

        // --- ACT ---
        List<Task> result = reviewerAgent.process(task);

        // --- ASSERT ---
        assertTrue(result.isEmpty());
        // Verify we did NOT call LLM or set status to COMPLETED
        verifyNoInteractions(llmService);
        verify(storage, never()).save(contains(":status"), eq(WorkflowStatus.COMPLETED.name()));
    }

    @Test
    void process_ShouldComplete_WhenAllResultsReady() {
        // --- ARRANGE ---
        String wfId = "wf-done";
        Task task = Task.builder().workflowId(wfId).userRequest("Summary").build();

        when(appConfig.getAgents().getReviewer().isEnabled()).thenReturn(true);
        when(storage.get(eq("wf:" + wfId + ":status"), eq(String.class))).thenReturn(Optional.of("IN_PROGRESS"));
        when(storage.get(eq("wf:" + wfId + ":manifest"), eq(String.class))).thenReturn(Optional.of("TOOL_A"));

        // Result is ready
        when(storage.get(eq("wf:" + wfId + ":res:TOOL_A"), eq(String.class))).thenReturn(Optional.of("Data A"));

        when(llmService.generate(anyString(), anyString())).thenReturn("Final Verdict");

        // --- ACT ---
        reviewerAgent.process(task);

        // --- ASSERT ---
        // 1. Verify Status updates
        verify(storage).save(eq("wf:" + wfId + ":status"), eq(WorkflowStatus.REVIEWING.name()));
        verify(storage).save(eq("wf:" + wfId + ":status"), eq(WorkflowStatus.COMPLETED.name()));
        verify(storage).save(eq("wf:" + wfId + ":review"), eq("Final Verdict"));

        // 2. Verify LLM Called
        verify(llmService).generate(anyString(), contains("Data A"));
    }

    @Test
    void process_ShouldSkip_IfAlreadyCompleted() {
        // --- ARRANGE ---
        String wfId = "wf-skip";
        Task task = Task.builder().workflowId(wfId).build();
        when(appConfig.getAgents().getReviewer().isEnabled()).thenReturn(true);

        // Status is ALREADY COMPLETED
        when(storage.get(eq("wf:" + wfId + ":status"), eq(String.class)))
                .thenReturn(Optional.of(WorkflowStatus.COMPLETED.name()));

        // --- ACT ---
        reviewerAgent.process(task);

        // --- ASSERT ---
        // Should return immediately without checking manifest
        verify(storage, never()).get(contains(":manifest"), any());
    }
}