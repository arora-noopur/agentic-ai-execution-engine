package com.artc.agentic_ai_platform.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErpFetcherToolTest {

    private ErpFetcherTool tool;

    @BeforeEach
    void setup() {
        tool = new ErpFetcherTool();
    }

    @Test
    void getName_ShouldReturnCorrectName() {
        assertEquals("ERP_FETCHER", tool.getName());
    }

    @Test
    void execute_ShouldReturnJsonWithCorrectAssetId() {
        // ARRANGE
        String assetId = "PRESS-01";

        // ACT
        String result = tool.execute(assetId);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.contains("\"asset_id\": \"PRESS-01\""), "Response should contain the requested Asset ID");
        assertTrue(result.contains("maintenance_schedule"), "Response should contain maintenance data");
    }

    @Test
    void execute_ShouldHandleCompoundInput_AndExtractAssetId() {
        // ARRANGE: Input format "ID|CONTEXT"
        // The tool must split this and use only "PRESS-01" for the lookup
        String input = "PRESS-01|overheat";

        // ACT
        String result = tool.execute(input);

        // ASSERT
        assertTrue(result.contains("\"asset_id\": \"PRESS-01\""), "Should correctly parse ID before the pipe '|'");
    }

}