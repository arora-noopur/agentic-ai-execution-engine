package com.artc.agentic_ai_platform.model;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowResponse {
    private String workflowId;
    private String status;
    private String message;
}