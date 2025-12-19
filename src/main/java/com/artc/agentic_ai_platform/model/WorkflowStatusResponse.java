package com.artc.agentic_ai_platform.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // prevents null fields from showing in JSON
public class WorkflowStatusResponse {
    private String workflowId;
    private String status;
    private String finalDecision;
}
