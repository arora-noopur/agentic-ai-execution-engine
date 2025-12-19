package com.artc.agentic_ai_platform.controller;

import com.artc.agentic_ai_platform.constants.AppConstants;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.model.WorkflowStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final IStorageBackend storage;

    @GetMapping("/{workflowId}/status")
    public ResponseEntity<WorkflowStatusResponse> getStatus(@PathVariable String workflowId) {

        // Fetch status from storage
        String statusKey = String.format(AppConstants.KEY_STATUS, workflowId);
        String status = storage.get(statusKey, String.class).orElse("UNKNOWN");

        // Fetch final decision (if available)
        String reviewKey = String.format(AppConstants.KEY_REVIEW, workflowId);
        String decision = storage.get(reviewKey, String.class).orElse(null);

        // Build Response
        WorkflowStatusResponse response = WorkflowStatusResponse.builder()
                .workflowId(workflowId)
                .status(status)
                .finalDecision(decision)
                .build();

        return ResponseEntity.ok(response);
    }
}
