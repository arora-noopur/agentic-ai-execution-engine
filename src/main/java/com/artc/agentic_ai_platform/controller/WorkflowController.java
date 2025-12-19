package com.artc.agentic_ai_platform.controller;

import com.artc.agentic_ai_platform.core.IStorageBackend;
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
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String workflowId) {

        // 1. Fetch status from storage
        String statusKey = "wf:" + workflowId + ":status";
        Optional<String> statusOpt = storage.get(statusKey, String.class);
        String status = statusOpt.orElse("UNKNOWN");

        // 2. Fetch final decision/answer (if available)
        String reviewKey = "wf:" + workflowId + ":review";
        Optional<String> decisionOpt = storage.get(reviewKey, String.class);

        // 3. Construct Response (Using HashMap to allow conditional fields)
        Map<String, String> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("status", status);

        // Only include the decision if it exists (e.g., when COMPLETED)
        decisionOpt.ifPresent(decision -> response.put("finalDecision", decision));

        return ResponseEntity.ok(response);
    }
}
