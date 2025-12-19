package com.artc.agentic_ai_platform.tools;

import com.artc.agentic_ai_platform.core.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class ErpFetcherTool implements IAgentTool {
    @Override
    public String getName() { return "ERP_FETCHER"; }

    @Override
    public String execute(String input) {

        // Parse input
        String[] parts = input.split("\\|");
        String assetId = parts[0].trim();

        log.info("[ERP_FETCHER] Connecting to Plant Maintenance Database for Asset: {}", assetId);

        return generateMockResponse(assetId);
    }

    private String generateMockResponse(String assetId) {
       return String. format(
            """
              {
              "asset_id": "%s",
              "purchase_date": "2018-05-20",
              "maintenance_schedule": {
                "last_service": "2022-01-01",
                "next_due": "2023-01-01",
                "status": "OVERDUE_FLAGS_ACTIVE"
              }
            }""", assetId);
    }

}
