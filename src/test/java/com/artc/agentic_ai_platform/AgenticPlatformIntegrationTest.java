package com.artc.agentic_ai_platform;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AgenticPlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // NO @MockBean here!
    // We are using the real MockLLMService, real Agents, and real In-Memory Storage.

    @Test
    void testEndToEndWorkflow_WithRealComponents() throws Exception {

        // 1. ACT: Trigger the Incident
        // We use a keyword "overheat" that we know the Worker/Tools will pick up
        String payload = "URGENT: Machine PRESS-01 is reporting an overheat error code.";

        MvcResult postResult = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.workflowId").exists())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andReturn();

        // Extract ID
        String responseBody = postResult.getResponse().getContentAsString();
        String workflowId = responseBody.split("\"workflowId\":\"")[1].split("\"")[0];

        // 2. ASSERT: Poll for Completion
        boolean isCompleted = false;

        // Poll for up to 5 seconds
        for (int i = 0; i < 10; i++) {
            MvcResult statusResult = mockMvc.perform(get("/api/workflows/" + workflowId + "/status"))
                    .andReturn();

            String content = statusResult.getResponse().getContentAsString();

            // Check if status reached COMPLETED
            if (content.contains("COMPLETED")) {
                isCompleted = true;

                // 3. Verify the MockLLMService logic actually ran
                // The MockLLMService returns "FINAL DECISION: IMMEDIATE SHUTDOWN (Overheat + Missed Maintenance)."
                if (!content.contains("IMMEDIATE SHUTDOWN")) {
                    throw new RuntimeException("Workflow finished but final decision is wrong! Got: " + content);
                }
                break;
            }
            Thread.sleep(500);
        }

        if (!isCompleted) {
            throw new RuntimeException("Test Failed: Workflow stuck in PENDING/RUNNING state.");
        }
    }
}
