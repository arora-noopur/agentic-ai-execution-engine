package com.artc.agentic_ai_platform.core.llm;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class MockLLMService implements ILlmService {

    private final Random random = new Random();

    public String generate(String systemPrompt, String userPrompt) {
        simulateLatency();
        String userPromptLower = userPrompt.toLowerCase();

        // --- 1. PLANNER LOGIC (Rich Decomposition) ---
        if (systemPrompt.contains("Planner Agent")) {
            if (userPromptLower.contains("overheat")) {
                // Returns specific files to enable parallel processing in Worker
                return """
                    {
                      "reasoning": "Detected thermal anomaly. Scanning logs and checking maintenance.",
                      "steps": [
                        {
                          "tool": "LOG_ANALYZER",
                          "inputs": ["/var/logs/sensor_primary.log", "/var/logs/sensor_backup.log", "/var/logs/system_events.log"]
                        },
                        {
                          "tool": "ERP_FETCHER",
                          "inputs": ["PRESS-01"]
                        }
                      ]
                    }
                    """;
            } else {
                // Default fallback plan
                return """
                    {
                      "reasoning": "General diagnostics required.",
                      "steps": [
                        { "tool": "LOG_ANALYZER", "inputs": ["/var/logs/general.log"] }
                      ]
                    }
                    """;
            }
        }

        // --- 2. WORKER LOGIC (Context Aware) ---
        if (systemPrompt.contains("Worker Agent")) {
            // Logic to detect "CRITICAL" in raw output
            if (userPrompt.contains("CRITICAL") || userPrompt.contains("150C")) {
                return "INSIGHT (Critical): Thermal Runaway pattern detected. Risk: HIGH.";
            }
            if (userPrompt.contains("OVERDUE")) {
                return "INSIGHT (Maintenance): Service is 6 months overdue.";
            }
            return "INSIGHT: No anomalies found in this chunk.";
        }

        // --- 3. REVIEWER LOGIC ---
        if (systemPrompt.contains("Reviewer Agent")) {
            if (userPrompt.contains("Thermal Runaway") && userPrompt.contains("overdue")) {
                return "FINAL DECISION: IMMEDIATE SHUTDOWN (Overheat + Missed Maintenance).";
            }
            return "FINAL DECISION: MONITOR. No critical combined failures.";
        }

        return "I am unsure.";
    }

    private void simulateLatency() {
        try { Thread.sleep(300 + random.nextInt(1000)); }
        catch (InterruptedException e) {}
    }
}