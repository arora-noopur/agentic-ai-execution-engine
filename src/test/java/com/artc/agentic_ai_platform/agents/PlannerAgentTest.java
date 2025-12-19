package com.artc.agentic_ai_platform.agents;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.llm.ILlmService;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.model.WorkflowStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlannerAgentTest {

    @Mock private ILlmService llmService;
    @Mock private IStorageBackend storage;

    // Mocks the config chain: appConfig.getAgents().getPlanner().isEnabled()
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    private PlannerAgent plannerAgent;

    @BeforeEach
    void setup() {
        // Use real ObjectMapper to test actual JSON parsing logic
        plannerAgent = new PlannerAgent(llmService, new ObjectMapper(), storage, appConfig);
    }

    @Test
    void process_ShouldGeneratePlan_AndSaveManifest() {
        // --- ARRANGE ---
        String wfId = "wf-101";
        Task inputTask = Task.builder()
                .workflowId(wfId)
                .userRequest("Check logs and database")
                .build();

        // 1. Mock Config Enabled
        when(appConfig.getAgents().getPlanner().isEnabled()).thenReturn(true);

        // 2. Mock LLM Response (Valid JSON)
        String jsonResponse = """
            {
                "steps": [
                    { "tool": "LOG_ANALYZER", "inputs": ["error.log"] },
                    { "tool": "DB_CHECKER", "inputs": ["users"] }
                ]
            }
        """;
        when(llmService.generate(anyString(), anyString())).thenReturn(jsonResponse);

        // --- ACT ---
        List<Task> resultTasks = plannerAgent.process(inputTask);

        // --- ASSERT ---
        // 1. Check generated tasks
        assertEquals(2, resultTasks.size());

        Task t1 = resultTasks.get(0);
        assertEquals(AgentType.WORKER, t1.getTargetAgent());
        assertEquals("LOG_ANALYZER", t1.getToolName());
        assertEquals("error.log", t1.getToolArguments().get(0));
        assertEquals("Check logs and database", t1.getUserRequest());

        // 2. Check Storage Updates
        // Verify Manifest Saved
        verify(storage).save(eq("wf:" + wfId + ":manifest"), eq("LOG_ANALYZER,DB_CHECKER"));

        // Verify Status Transitions
        verify(storage).save(eq("wf:" + wfId + ":status"), eq(WorkflowStatus.PLANNING.name()));
        verify(storage).save(eq("wf:" + wfId + ":status"), eq(WorkflowStatus.IN_PROGRESS.name()));
    }
}