package com.artc.agentic_ai_platform.controller;

import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.ITaskQueue;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.model.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {

    private final ITaskQueue queue;
    private final IStorageBackend storage;

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody String payload) {
        String wfId = UUID.randomUUID().toString();

        log.info("Received Incident Report. Workflow ID: {}", wfId);

        // 1. Set Status to PENDING immediately
        storage.save("wf:" + wfId + ":status", WorkflowStatus.PENDING.name(), 3600);

        // 2. Push to Queue
        queue.push(Task.builder()
                .workflowId(wfId)
                .taskId(UUID.randomUUID().toString())
                .targetAgent(AgentType.PLANNER)
                .userRequest(payload)
                .build());

        // 3. Build Response Body
        Map<String, String> responseBody = Map.of(
                "workflowId", wfId,
                "status", WorkflowStatus.PENDING.name(),
                "message", "Incident Accepted for processing"
        );

        // 4. Return 202 ACCEPTED
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(responseBody);
    }
}