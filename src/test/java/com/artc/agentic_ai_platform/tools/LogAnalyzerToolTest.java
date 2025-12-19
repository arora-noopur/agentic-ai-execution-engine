package com.artc.agentic_ai_platform.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogAnalyzerToolTest {

    private LogAnalyzerTool tool;

    @BeforeEach
    void setup() {
        tool = new LogAnalyzerTool();
    }

    @Test
    void getName_ShouldReturnCorrectToolName() {
        assertEquals("LOG_ANALYZER", tool.getName());
    }

    @Test
    void execute_ShouldFilterBySpecificKeyword() {
        //Search for "drift" (exists in WARN log)
        String input = "server.log|drift";

        String result = tool.execute(input);

        assertTrue(result.contains("calibration drift 2%"), "Should return the line containing 'drift'");
        // Verify we aren't getting everything
        assertFalse(result.contains("System started"), "Should NOT return unrelated INFO logs");
    }

    @Test
    void execute_ShouldDefaultToErrorContext_WhenNoKeywordProvided() {
        // No pipe | keyword provided
        String input = "server.log";

        String result = tool.execute(input);

        assertTrue(result.contains("CRITICAL"), "Should return CRITICAL errors by default");
        assertTrue(result.contains("SAFETY_LOCK"), "Should return SAFETY_LOCK errors by default");
        assertFalse(result.contains("Heartbeat"), "Should exclude DEBUG logs");
    }

    @Test
    void execute_ShouldAlwaysIncludeCriticalErrors_EvenWithUnrelatedKeyword() {
        // test for tool logic -> (contains(keyword) || contains("error"))
        String input = "server.log|banana";

        String result = tool.execute(input);

        // Should still return critical errors because they are hardcoded to be relevant
        assertTrue(result.contains("CRITICAL"));
        assertTrue(result.contains("Temperature sensor"), "Critical errors should always surface");
    }

    @Test
    void execute_ShouldBeCaseInsensitive() {
        //Search for "DRIFT" (Upper case)
        String input = "server.log|DRIFT";

        String result = tool.execute(input);

        assertTrue(result.contains("drift 2%"), "Should match 'drift' even if input is uppercase");
    }

    @Test
    void execute_ShouldReturnInfoLogs_WhenKeywordMatchesExplicitly() {
        //"started" (exists in INFO log)
        String input = "server.log|started";

        String result = tool.execute(input);

        assertTrue(result.contains("System started"), "Should return INFO logs if explicitly requested");
    }
}