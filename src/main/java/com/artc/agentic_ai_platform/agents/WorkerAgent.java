package com.artc.agentic_ai_platform.agents;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IAgent;
import com.artc.agentic_ai_platform.core.IAgentTool;
import com.artc.agentic_ai_platform.core.exception.TerminalException;
import com.artc.agentic_ai_platform.core.llm.MockLLMService;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkerAgent implements IAgent {

    private final Map<String, IAgentTool> toolMap;
    private final IStorageBackend storage;
    private final MockLLMService llmService; // Inject the Brain
    private final Executor executor;
    private final AppConfig appConfig;

    public WorkerAgent(List<IAgentTool> tools, IStorageBackend storage, MockLLMService llmService, @Qualifier("workerInternalExecutor") Executor executor, AppConfig appConfig) {
        this.storage = storage;
        this.llmService = llmService;
        this.toolMap = tools.stream()
                .collect(Collectors.toMap(IAgentTool::getName, Function.identity()));
        this.executor = executor;
        this.appConfig = appConfig;

        log.info("[WORKER] Loaded {} tools: {}", toolMap.size(), toolMap.keySet());
    }

    @Override
    public AgentType getType() { return AgentType.WORKER; }

    @Override
    public List<Task> process(Task task) {

        // Runtime Gate (Configuration check)
        if(!appConfig.getAgents().getWorkers().isEnabled()) {
            log.warn("[WORKER] Disabled by config. Dropping task: {}", task.getTaskId());
            return List.of();
        }

        List<String> itemsToProcess = task.getToolArguments();
        String context = task.getUserRequest();
        String toolName = task.getToolName();

        log.info("[WORKER] Executing tool '{}' on {} items. Context: {}", toolName, itemsToProcess.size(), context);

        IAgentTool tool = toolMap.get(task.getToolName());
        if(tool == null) throw new TerminalException("Tool not found: " + task.getToolName());

        // --- Parallel Execution (Scatter) for all items in this tool call ---
        // Fan out tasks to internal executor
        List<CompletableFuture<String>> futures = itemsToProcess.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> executeSingleItem(tool,item,context), executor))
                .toList();

        // --- Aggregation (Gather) ---
        // Wait for all to finish
        String aggregatedResult = futures.stream()
                .map(CompletableFuture::join)
                        .collect(Collectors.joining("\n --------- \n"));

        // --- Save Result ---
        storage.save("wf:" + task.getWorkflowId() + ":res:" + task.getToolName(), aggregatedResult);

        // --- Trigger Reviewer ---
        return List.of(Task.builder()
                .workflowId(task.getWorkflowId())
                .taskId(UUID.randomUUID().toString())
                .targetAgent(AgentType.REVIEWER)
                .userRequest(task.getUserRequest())
                .build());
    }

    // Helper function: Runs on a separate thread
    private String executeSingleItem(IAgentTool tool, String item, String context) {
        try {

            // 1. Tool Execution
            // Optimization: Pass Keyword if available to filter locally
            String toolInput = item + "|" + extractKeyword(context);
            String toolOutput = tool.execute(toolInput);

            // 2. AI Analysis (with context)
            String systemPrompt = "You are an intelligent Worker Agent. Analyze this specific data chunk.";
            String userPrompt = String.format("Context: %s\nData Source: %s\nRaw Output: %s",
                    context, tool.getName()+": " + item, toolOutput);

            return llmService.generate(systemPrompt, userPrompt);
        } catch(Exception e) {
            log.error("Error processing item {}: {}", item, e.getMessage());
            return "Error analyzing " + item;
        }
    }

    private String extractKeyword(String context) {
        if(context == null) return "";
        if(context.toLowerCase().contains("overheat")) return "overheat";
        if(context.toLowerCase().contains("vibration")) return "vibration";
        return "error";
    }
}
