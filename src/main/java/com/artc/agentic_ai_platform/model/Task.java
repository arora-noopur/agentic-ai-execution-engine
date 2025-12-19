package com.artc.agentic_ai_platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task implements Serializable {
    private String taskId;
    private String workflowId; // Links all tasks in one incident
    private AgentType targetAgent;
    // 1. The "Why" (Context). Passed down to every agent.
    private String userRequest;

    // Optional: Only for WORKER
    private String toolName;
    // 2. The "What" (Data). Specific items for this agent to work on.
    @Builder.Default
    private List<String> toolArguments = new ArrayList<>();

    // Reliability Fields
    private int retryCount;
    private long nextRetryTimestamp;
}
