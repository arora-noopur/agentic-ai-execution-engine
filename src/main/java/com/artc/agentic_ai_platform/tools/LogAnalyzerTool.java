package com.artc.agentic_ai_platform.tools;

import com.artc.agentic_ai_platform.core.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class LogAnalyzerTool implements IAgentTool {

    // Simulating a huge file on disk
    private static final String MOCK_HUGE_LOG_FILE = """
            [10:00:00] INFO System started
            [10:00:01] INFO Connection established
            [10:00:02] DEBUG Heartbeat received
            [10:00:03] WARN Sensor calibration drift 2%
            [10:00:04] INFO User logged in
            [10:00:05] ERROR CRITICAL: Temperature sensor reporting 150C
            [10:00:06] ERROR SAFETY_LOCK: Emergency shutdown triggered
            [10:00:07] DEBUG Dump saved
            [10:00:08] INFO System restarting...
            """;

    @Override
    public String getName() { return "LOG_ANALYZER"; }

    /**
     * @param input A JSON string or simple string containing parameters.
     * Format expected: "FILENAME|CONTEXT_KEYWORDS"
     */
    @Override
    public String execute(String input) {
        String[] parts = input.split("\\|");
        String fileName = parts[0];
        String contextKeyword = parts.length > 1 ? parts[1].toLowerCase() : "error";

        log.info("[TOOL] Streaming file: {} looking for '{}'...", fileName, contextKeyword);

        // 1. STREAMING PROCESS (Simulated)
        // Actual: Files.lines(Paths.get(filename))
        Stream<String> logStream = new BufferedReader(new StringReader(MOCK_HUGE_LOG_FILE)).lines();

        // 2. FILTER LOCALLY (Heuristic Layer)
        String relevantChunk = logStream
                .filter(line -> isRelevant(line, contextKeyword))
                .limit(50)
                .collect(Collectors.joining("\n"));

        if(relevantChunk.isEmpty()) return "No relevant log lines found for keyword: " + contextKeyword;

        return relevantChunk;
    }

    private boolean isRelevant(String line, String keyword) {
        String lower = line.toLowerCase();
        return lower.contains(keyword) || lower.contains("error") || lower.contains("critical");
    }
}
