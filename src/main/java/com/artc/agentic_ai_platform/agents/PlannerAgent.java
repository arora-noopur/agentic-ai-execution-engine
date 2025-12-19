package com.artc.agentic_ai_platform.agents;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IAgent;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.llm.ILlmService;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.model.WorkflowStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlannerAgent implements IAgent {

    private final ILlmService llmService;
    private final ObjectMapper objectMapper;
    private final IStorageBackend storage;
    private final AppConfig appConfig;

    @Override
    public AgentType getType() { return AgentType.PLANNER; }

    @Override
    public List<Task> process(Task task) {

        // 1. Runtime gate (Configuration check)
        if(!appConfig.getAgents().getPlanner().isEnabled()) {
            log.warn("[PLANNER] Disabled by config. Dropping task: {}", task.getTaskId());
            return List.of();
        }

        storage.save("wf:" + task.getWorkflowId() + ":status", WorkflowStatus.PLANNING.name());

        log.info("[PLANNER] Prompting LLM for incident: {}", task.getUserRequest());

        // 2. Construct the Prompt (Prompt Engineering)
        String systemPrompt = "You are a Planner Agent in a smart factory. Output JSON only. Available tools: [LOG_ANALYZER, ERP_FETCHER].";
        String userPrompt = "Incident Report: " + task.getUserRequest();

        // 3. Call the AI
        String aiResponse = llmService.generate(systemPrompt, userPrompt);
        log.info("[PLANNER] AI Reasoning: {}", aiResponse);

        // 4. Parse AI Output (JSON extraction) and create tasks accordingly
        List<String> expectedTools = new ArrayList<>();
        List<Task> downstreamTasks = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode steps = root.get("steps");

            if(steps!=null && steps.isArray()) {
                for(JsonNode step: steps) {
                    String toolName = step.get("tool").asText();

                    List<String> inputs = new ArrayList<>();

                    if(step.has("inputs") && step.get("inputs").isArray()) {
                        for(JsonNode input: step.get("inputs")) {
                            inputs.add(input.asText());
                        }
                    } else {
                        inputs.add("DEFAULT_SCAN");
                    }

                    expectedTools.add(toolName);
                    downstreamTasks.add(Task.builder()
                            .workflowId(task.getWorkflowId())
                            .taskId(UUID.randomUUID().toString())
                            .userRequest(task.getUserRequest())
                            .targetAgent(AgentType.WORKER)
                            .toolName(toolName)
                            .toolArguments(inputs)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            // Fallback strategy could go here
        }

        // 5. Save Manifest (The "Contract" for the Reviewer) & the updated workflow status
        // Key: wf:{id}:manifest -> Value: ["LOG_ANALYZER", "ERP_FETCHER"]
        String manifestStr = String.join(",", expectedTools);
        storage.save("wf:" + task.getWorkflowId() + ":manifest", manifestStr);
        storage.save("wf:" + task.getWorkflowId() + ":status", WorkflowStatus.IN_PROGRESS.name());
        return downstreamTasks;
    }
}
