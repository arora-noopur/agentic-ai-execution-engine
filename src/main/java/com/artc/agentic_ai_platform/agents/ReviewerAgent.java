package com.artc.agentic_ai_platform.agents;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IAgent;
import com.artc.agentic_ai_platform.core.llm.MockLLMService;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.model.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewerAgent implements IAgent {

    private final IStorageBackend storage;
    private final MockLLMService llmService;
    private final AppConfig appConfig;

    @Override
    public AgentType getType() { return AgentType.REVIEWER; }

    @Override
    public List<Task> process(Task task) {

        // --- 1. Runtime Gate (Configuration check)
        if(!appConfig.getAgents().getReviewer().isEnabled()) {
            log.info("[REVIEWER] Disabled by config. Skipping analysis for workflow: {}", task.getWorkflowId());
            storage.save("wf:"+ task.getWorkflowId() + ":status", WorkflowStatus.COMPLETED_NO_REVIEW.name());
            return List.of();
        }

        String wfId = task.getWorkflowId();

        // --- 2. Idempotency check (Prevent Double Processing) ---
        Optional<String> status = storage.get("wf:" + wfId + ":status", String.class);
        if (status.isPresent()) {
            String current = status.get();
            if (WorkflowStatus.COMPLETED.name().equals(current) || WorkflowStatus.REVIEWING.name().equals(current)) {
                log.info("[REVIEWER] Workflow {} is already completed. Ignoring duplicate trigger.", wfId);
                return List.of();
            }
        }

        // --- 3. Fetch Manifest (What are we waiting for ?) ---
        Optional<String> manifestOpt = storage.get("wf:" + wfId + ":manifest", String.class);
        if(manifestOpt.isEmpty()) {
            log.warn("[REVIEWER] Manifest missing for {}. Planner might be slow", wfId);
            return List.of(); // Wait for Planner to save manifest
        }

        String[] expectedTools = manifestOpt.get().split(",");
        Map<String, String> collectedResults = new HashMap<>();

        // --- 4. Dynamic Data Completeness check ---
        for(String toolName: expectedTools) {
            Optional<String> result = storage.get("wf:"+wfId+":res:"+toolName, String.class);

            if(result.isEmpty()) {
                log.info("[REVIEWER] Waiting for tool: {}", toolName);
                return List.of();
            }
            collectedResults.put(toolName, result.get());
        }

        // --- 5. Critical Section (The Execution) ---
        // Lock the workflow status immediately
        storage.save("wf:" + wfId + ":status", WorkflowStatus.REVIEWING.name());
        log.info("[REVIEWER] All data gathered. Executing AI Analysis...");

        // --- 6. Build Dynamic Prompt ---
        String findings = collectedResults.entrySet().stream()
                .map(e -> String.format(" FINDING (%s): %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));

        String userPrompt = String.format("""
                ORIGINAL INTENT: %s
                
                %s
                
                Synthesize these findings into a final recommendation.
                """, task.getUserRequest(), findings);

        String systemPrompt = "Reviewer Agent System Prompt";

        String decision = llmService.generate(systemPrompt, userPrompt);

        storage.save("wf:" + wfId + ":review", decision);
        storage.save("wf:" + wfId + ":status", WorkflowStatus.COMPLETED.name());

        log.info("#######################################################");
        log.info("FINAL DECISION: {}", decision);
        log.info("#######################################################");

        return List.of();
    }
}