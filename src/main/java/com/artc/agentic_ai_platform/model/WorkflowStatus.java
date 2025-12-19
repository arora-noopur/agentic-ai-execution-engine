package com.artc.agentic_ai_platform.model;

public enum WorkflowStatus {
    PENDING,              // Created but not picked up yet
    PLANNING,             // Planner is thinking
    IN_PROGRESS,          // Workers are executing tools
    REVIEWING,            // Reviewer is analyzing results
    COMPLETED,            // Finished successfully
    COMPLETED_NO_REVIEW,  // Skipped review (config disabled)
    FAILED                // Something went wrong
}
